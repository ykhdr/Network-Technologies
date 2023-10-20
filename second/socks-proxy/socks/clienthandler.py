import socket
import select
import struct
from struct import unpack

import dns

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
        if method == M_NOAUTH:
            return M_NOAUTH

    return M_NOTAVAILABLE


def accept_new_client(client, data):
    method = client_greeting(data)

    reply = VER + method
    client.sendall(reply)

    return method == M_NOTAVAILABLE


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
    dst_port = unpack('>H', message[5 + domain_length:len(message)])[0]

    return domain_name, dst_port


def create_dns_query(domain):
    request = dns.message.make_query(domain, dns.rdatatype.A)
    request.id = IdGenerator.next_id()
    return request


def response_on_connection_request(client, status, atype, bound_address, bound_port):
    reply = VER + status + RSV + atype + bound_address + struct.pack('BB', bound_port)
    client.send(reply)


def handle_clients():
    server = init_server()
    dns_sock = init_dns()
    clients = []
    destinations = []
    clients_with_dst = {}
    clients_waiting_dns = {}
    inputs = [server, dns_sock]

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
                response = dns.message.from_wire(response_data)
                answer = response.answer[0]
                if answer.rdtype == dns.rdatatype.A:
                    print(f"Resolved IP for ID {response.id}: {answer[0].address}")
                else:
                    print("error, rdtype != ipv4")
                client = clients_waiting_dns[response.id]
                clients_with_dst[client] = answer[0].address


            else:
                # Клиент хочет отправить сообщение
                response_data = sock.recv(BUFFER_SIZE)
                if not response_data:
                    # Клиент выходит
                    print(f'Client {sock.getpeername()} disconnected')
                    inputs.remove(sock)
                    sock.close()

                if sock not in clients:
                    # первое сообщение от клиента после коннекта
                    if accept_new_client(sock, response_data):
                        # проверяем правильная ли версия socks пришла от клиента и метод
                        print(f'Client {sock.getpeername()} accepted')
                        clients.append(sock)
                    else:
                        print(f'Client {sock.getpeername()} did not accept')
                        inputs.remove(sock)
                elif sock not in clients_with_dst.keys():
                    # Если клиент еще не отправил сообщение с адресом
                    dst_atype = accept_client_connection(response_data)

                    if dst_atype == ATYPE_IPV4:
                        dst_addr = socket.inet_ntoa(response_data[4:-2])
                        dst_port = unpack('>H', response_data[8:])[0]

                        dst_sock = init_dst_sock(dst_addr, dst_port)
                        clients_with_dst[sock] = dst_sock
                        response_on_connection_request(sock, ST_REQUEST_GRANTED, dst_atype, dst_addr, dst_port)

                    elif dst_atype == ATYPE_DOMAINNAME:
                        domain_name, dst_port = resolve_domain_name(response_data)
                        dns_query = create_dns_query(domain_name)
                        dns_sock.sendall(dns_query.to_wire())
                        clients_waiting_dns[dns_query.id] = sock
                    else:
                        response_on_connection_request(sock, ST_PROTOCOL_ERROR, dst_atype)

                elif sock in clients_with_dst.values():
                    # Пришло сообщение от dst

                    pass
                else:
                    dest = clients_with_dst[sock]
                    if not dest:
                        print(f'Error with destination of client {sock.getppername()}')
                        # TODO сообщить клиенту об ошибке и закрыть соединение
                    else:
                        dest.sendall(response_data)


def init_dst_sock(dst_addr, dst_port):
    dst_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

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
    dns = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    dns.connect((DNS_ADDRESS, DNS_PORT))
    dns.setblocking(False)

    return dns
