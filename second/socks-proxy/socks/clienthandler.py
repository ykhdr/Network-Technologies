import socket
import select
import string
import struct
import traceback
from struct import unpack

import dns
from dns import message

# CONFIG
SERVER_PORT = 1080
SERVER_ADDRESS = '0.0.0.0'

DNS_PORT = 53
DNS_ADDRESS = '8.8.8.8'

# PROTOCOL PARAMETERS
VER = b'\x05'
'''Protocol version'''

RSV = b'\x00'
'''Reserved, must be 0x00'''

# Only available at this program
CMD_ESTABLISH_TCPIP = b'\x01'
'''Command code of establish a TCP/IP stream connection'''

# Only no auth possible now
M_NOAUTH = b'\x00'
'''Authentication method'''

M_NOTAVAILABLE = b'\xff'
'''Requested methods are not available'''

# STATUS CODE
ST_REQUEST_GRANTED = b'\x00'
'''Status request granted'''''

ST_GENERAL_FAILURE = b'\x01'
'''Status general failure'''

ST_PROTOCOL_ERROR = b'\x07'
'''Status command not supported / protocol error'''

# DEFAULT TYPES
ATYPE_IPV4 = b'\x01'
'''IPv4 address (01)'''

ATYPE_DOMAINNAME = b'\x03'
'''Domain name (03)'''

# UTILS
BUFFER_SIZE = 4 * 1024


class IdGenerator:
    __next_id = 0

    @staticmethod
    def next_id():
        cur_id = IdGenerator.__next_id
        if IdGenerator.__next_id + 1 == 65535:
            IdGenerator.__next_id = 0
        else:
            IdGenerator.__next_id += 1
        return cur_id


def client_greeting(message):
    if VER != message[0:1]:
        return M_NOTAVAILABLE

    n_methods = message[1]
    methods = message[2:]
    if n_methods != len(methods):
        return M_NOTAVAILABLE

    for method in methods:
        if method == ord(M_NOAUTH):
            return M_NOAUTH

    return M_NOTAVAILABLE


def accept_new_client(client, data):
    method = client_greeting(data)

    reply = VER + method
    print(reply)
    client.sendall(reply)

    return method != M_NOTAVAILABLE


def accept_client_connection(message):
    f"""
    return dst_addr, dst_port, status \n
    exception {SyntaxError} if error in message syntax
    """
    #   +-----+-----+-------+------+----------+----------+
    #   | VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
    #   +-----+-----+-------+------+----------+----------+
    # TODO проверить на правильность сообщения о запросе подключения и в случае успеха совершить подключение и
    #      вернуть сокет

    # TODO проверить на длину сообщения  (minimum посчитать) или же сделать обработку ошибки

    if (
            VER != message[0:1] or
            CMD_ESTABLISH_TCPIP != message[1:2] or
            RSV != message[2:3]
    ):
        return None

    if message[3:4] == ATYPE_IPV4:
        return ATYPE_IPV4
    elif message[3:4] == ATYPE_DOMAINNAME:
        return ATYPE_DOMAINNAME
    else:
        return None


def resolve_domain_name(message):
    domain_length = message[4]
    # TODO протестить правильно ли работает
    domain_name = message[5:5 + domain_length]
    print('HHH' + str(domain_name) + 'HHH')
    dst_port = unpack('>H', message[5 + domain_length:len(message)])[0]

    return domain_name, dst_port


def create_dns_query(domain):
    dns_id = IdGenerator.next_id()
    # TODO проверить domain на то что норм ли ему в строке
    dns_query = dns.message.make_query(str(domain), dns.rdatatype.A, id=dns_id)
    dns_query.flags |= dns.flags.CD | dns.flags.AD
    print('DNS QUERY:' + str(dns_query))
    return dns_query, dns_id


def response_on_connection_request(client, status, atype, bound_address, bound_port):
    reply = VER + status + RSV + atype + bound_address + struct.pack('>H', bound_port)
    client.send(reply)
    print(f'Response on client\'s connection {client.getpeername()} sent')


def resolve_dns_response(response_data):
    response = dns.message.from_wire(response_data)
    print('DNS RESPONSE:' + str(response))

    if response.answer:
        return response.answer[0][0].address, response.id
    else:
        return None, response.id


def handle_clients():
    server = init_server()
    dns_sock = init_dns()
    clients = []

    # сокет клиента : сокет дистанта
    clients_with_dst = {}

    # сокет дистанта : сокет клиента
    dst_with_clients = {}

    # Id днс : сокет клиента
    clients_waiting_dns = {}

    # Id днс : порт дистанта
    dns_id_with_dst_port = {}
    inputs = [server, dns_sock]

    print(f'Server start receiving messages on address {server.getsockname()}')

    while True:
        reads, _, _ = select.select(inputs, [], [])
        for sock in reads:
            if sock == server:
                # Клиент хочет присоединиться
                client, addr = server.accept()
                print(f"New connection: {addr}")

                inputs.append(client)
            elif sock == dns_sock:
                # Днс прислал ответ

                response_data = dns_sock.recv(BUFFER_SIZE)
                dst_addr, dns_id = resolve_dns_response(response_data)

                if not dst_addr:
                    print('No address for this domain')
                    del dns_id_with_dst_port[dns_id]
                    continue

                dst_port = dns_id_with_dst_port[dns_id]
                dst_sock = init_dst_sock(dst_addr, dst_port)
                response_on_connection_request(sock, ST_REQUEST_GRANTED, ATYPE_DOMAINNAME, dst_addr, dst_port)
                inputs.append(dst_sock)
                client = clients_waiting_dns[dns_id]
                print(f'Dns server answered to {client.address}')
                clients_with_dst[client] = dst_sock
                dst_with_clients[dst_sock] = client
                del dns_id_with_dst_port[dns_id]
            else:
                # Клиент хочет отправить сообщение
                response_data = sock.recv(BUFFER_SIZE)
                print('respnsed data:' + str(response_data))
                if False:
                    pass
                if not response_data:

                    if sock in clients:
                        # Выходит клиент
                        print(f'Client {sock.getsockname()} disconnected')

                        dst_sock = clients_with_dst.get(sock)

                        if dst_sock:
                            dst_sock.close()
                            del dst_with_clients[dst_sock]
                            del clients_with_dst[sock]

                        sock.close()
                    else:
                        # Выходит дистант
                        print(f'Destination {sock.getpeername()} disconnect')
                        client = dst_with_clients.get(sock)

                        if client:
                            client.close()
                            del dst_with_clients[sock]
                            del clients_with_dst[client]

                        sock.close()

                    inputs.remove(sock)

                elif sock not in clients:
                    # первое сообщение от клиента после коннекта

                    if accept_new_client(sock, response_data):
                        # проверяем правильная ли версия socks пришла от клиента и метод
                        print(f'Client {sock.getsockname()} accepted')
                        clients.append(sock)
                    else:
                        print(f'Client {sock.getsockname()} did not accept')
                        inputs.remove(sock)

                elif sock not in clients_with_dst.keys():
                    # Если клиент еще не отправил сообщение с адресом

                    dst_atype = accept_client_connection(response_data)
                    print(f'Client {sock} sent message about dest')
                    if dst_atype == ATYPE_IPV4:
                        # IPV4
                        dst_addr = socket.inet_ntoa(response_data[4:-2])
                        dst_port = unpack('>H', response_data[8:])[0]

                        dst_sock = init_dst_sock(dst_addr, dst_port)
                        clients_with_dst[sock] = dst_sock
                        dst_with_clients[dst_sock] = sock
                        response_on_connection_request(sock, ST_REQUEST_GRANTED, dst_atype, dst_addr, dst_port)
                        inputs.append(dst_sock)

                    elif dst_atype == ATYPE_DOMAINNAME:
                        # DOMAIN NAME
                        domain_name, dst_port = resolve_domain_name(response_data)
                        dns_query, dns_id = create_dns_query(domain_name)
                        # print(dns_query)
                        dns_sock.send(dns_query.to_wire())
                        # print(dns_query)
                        clients_waiting_dns[dns_id] = sock
                        dns_id_with_dst_port[dst_port] = dns_id
                        print(f'Client {sock.getsockname()} waiting a domain resolve from dns server')
                    else:
                        # UNSUPPORTED
                        # TODO подумать
                        print(f'Unknown atype, message: {response_data}')
                        # response_on_connection_request(sock, ST_PROTOCOL_ERROR, ATYPE_IPV4, )
                        # pass

                elif sock in dst_with_clients.keys():
                    # Пришло сообщение от dst

                    client = dst_with_clients[sock]
                    print(f'Received message from dest {sock.getsockname()} to client {client.getsockname()}')
                    client.sendall(response_data)

                else:
                    # Пришло сообщение От клиента дистанту

                    dst = clients_with_dst[sock]
                    print(f'Received message from client {sock.getsockname()} to dest {dst.getsockname()}')
                    if not dst:
                        error(f'Error with destination of client {sock.getppername()}')
                        # TODO сообщить клиенту об ошибке и закрыть соединение
                        sock.close()
                        dst.close()
                        inputs.remove(sock)
                        clients.remove(sock)
                        del clients_with_dst[sock]
                        del dst_with_clients[dst]
                    else:
                        dst.sendall(response_data)


def init_dst_sock(dst_addr, dst_port):
    dst_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    # TODO проверить на исключения
    dst_sock.connect((dst_addr, dst_port))
    dst_sock.setblocking(False)

    return dst_sock


def init_server():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    server.bind((SERVER_ADDRESS, SERVER_PORT))
    server.listen(5)
    server.setblocking(False)

    return server


def init_dns():
    dns_sck = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        dns_sck.connect((DNS_ADDRESS, DNS_PORT))
    except socket.error as err:
        print('err')

    dns_sck.setblocking(False)

    return dns_sck
