import socket
import select
import struct
import sys
from signal import signal, SIGINT, SIGTERM
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

ST_HOST_UNREACHABLE = b'\x04'
'''Status host unreachable'''

ST_COMMAND_NOT_SUPPORTED = b'\x07'
'''Status command not supported'''

# DEFAULT TYPES
ATYPE_IPV4 = b'\x01'
'''IPv4 address (01)'''

ATYPE_DOMAINNAME = b'\x03'
'''Domain name (03)'''

# UTILS
BUFFER_SIZE = 1024 * 1024
'''Buffer size'''


class IdGenerator:
    """ Generate ids for dns queries """
    __next_id = 0

    @staticmethod
    def next_id():
        """ get next id """
        cur_id = IdGenerator.__next_id
        if IdGenerator.__next_id + 1 == 65535:
            IdGenerator.__next_id = 0
        else:
            IdGenerator.__next_id += 1
        return cur_id


class ExitStatus:
    """ Manage exit status """

    def __init__(self):
        self.exit = False

    def set_status(self, status):
        """ set exist status """
        self.exit = status

    def get_status(self):
        """ get exit status """
        return self.exit


EXIT = ExitStatus()


def exit_handler(signum, frame):
    """handle signals"""
    print('Signal handler called with signal', signum)
    EXIT.set_status(True)


def client_greeting(data):
    """identifier client version"""
    # +----+----------+----------+
    # |VER | NMETHODS | METHODS  |
    # +----+----------+----------+

    if VER != data[0:1]:
        return M_NOTAVAILABLE

    n_methods = data[1]
    methods = data[2:]
    if n_methods != len(methods):
        return M_NOTAVAILABLE

    for method in methods:
        if method == ord(M_NOAUTH):
            return M_NOAUTH

    return M_NOTAVAILABLE


def accept_new_client(client, data):
    """replying on client greeting message"""
    # Server reply
    # +----+--------+
    # |VER | METHOD |
    # +----+--------+

    method = client_greeting(data)

    reply = VER + method
    client.sendall(reply)

    return method != M_NOTAVAILABLE


def accept_client_connection(data):
    """checking client request details"""
    # Client message structure
    # +-----+-----+-------+------+----------+----------+
    # | VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
    # +-----+-----+-------+------+----------+----------+

    if (
            VER != data[0:1] or
            CMD_ESTABLISH_TCPIP != data[1:2] or
            RSV != data[2:3]
    ):
        return None

    if data[3:4] == ATYPE_IPV4:
        return ATYPE_IPV4
    elif data[3:4] == ATYPE_DOMAINNAME:
        return ATYPE_DOMAINNAME
    else:
        return None


def response_on_connection_request(status, atype, dst_sock):
    """create response on client request details message"""
    # Server Reply
    # +----+-----+-------+------+----------+----------+
    # |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
    # +----+-----+-------+------+----------+----------+

    bnd = socket.inet_aton(dst_sock.getsockname()[0]) + struct.pack('>H', dst_sock.getsockname()[1])
    reply = VER + status + RSV + atype + bnd

    return reply


def resolve_domain_name(data):
    """resolving domain name from client message"""
    domain_length = data[4]
    domain_name = data[5:5 + domain_length]
    dst_port = unpack('>H', data[5 + domain_length:len(data)])[0]

    return domain_name, dst_port


def create_dns_query(domain):
    """create dns query on domain name from client"""
    dns_id = IdGenerator.next_id()
    dns_query = dns.message.make_query(str(domain)[2:-1], dns.rdatatype.A, id=dns_id)
    dns_query.flags |= dns.flags.CD | dns.flags.AD
    return dns_query, dns_id


def resolve_dns_response(response_data):
    """resolving domain name to (IPv4 address, dns request id) from dns response"""
    response = dns.message.from_wire(response_data)

    if response.answer:
        for answer in response.answer:
            if answer.rdtype == dns.rdatatype.A:
                return answer[0].address, response.id

    return None, response.id


def handle_new_client_connection(server, inputs):
    client, addr = server.accept()
    print(f'New connection: {addr}')

    inputs.append(client)


def handle_dns_response(dns_sock, inputs, dns_id_with_clients, dns_id_with_dst_port, clients_with_dst,
                        dst_with_clients):
    response_data = dns_sock.recv(BUFFER_SIZE)
    dst_addr, dns_id = resolve_dns_response(response_data)

    client = dns_id_with_clients.get(dns_id, None)

    if not client:
        print(f'Client does not wating dns', file=sys.stderr)
        del dns_id_with_dst_port[dns_id]
        del dns_id_with_clients[dns_id]
        return
    if not dst_addr:
        print(f'Dns does not know about domain for {client.getsockname()}', file=sys.stderr)

        reply = response_on_connection_request(ST_HOST_UNREACHABLE, ATYPE_IPV4, client)
    else:
        dst_port = dns_id_with_dst_port[dns_id]
        dst_sock = init_dst_sock(dst_addr, dst_port)
        if not dst_sock:
            reply = response_on_connection_request(ST_HOST_UNREACHABLE, ATYPE_IPV4, client)
        else:
            inputs.append(dst_sock)
            clients_with_dst[client] = dst_sock
            dst_with_clients[dst_sock] = client

            reply = response_on_connection_request(ST_REQUEST_GRANTED, ATYPE_IPV4, dst_sock)
    try:
        print(f'Dns received resolved domain for {client.getsockname()}')
        client.send(reply)
    except socket.error:
        print('Error while sending response message to client', file=sys.stderr)
        return
    del dns_id_with_dst_port[dns_id]
    del dns_id_with_clients[dns_id]


def handle_connection_reset_error(sock, inputs, clients, clients_with_dst, dst_with_clients):
    sock.close()
    inputs.remove(sock)
    if sock in clients:
        dst = clients_with_dst.get(sock)
        if dst:
            dst.close()
            del clients_with_dst[sock]
            del dst_with_clients[dst]
            inputs.remove(dst)
            clients.remove(sock)
    else:
        client = dst_with_clients.get(sock)
        client.close()
        del clients_with_dst[client]
        del dst_with_clients[sock]
        clients.remove(client)
        inputs.remove(client)

    print(f'Connection reset for sock', file=sys.stderr)


def handle_client_leave(client, clients, inputs, clients_with_dst, dst_with_clients):
    print(f'Client {client.getsockname()} disconnected')

    dst_sock = clients_with_dst.get(client, None)

    if dst_sock:
        dst_sock.close()

        del dst_with_clients[dst_sock]
        del clients_with_dst[client]
        if dst_sock in inputs:
            inputs.remove(dst_sock)

    clients.remove(client)


def handle_dst_leave(dst, clients, inputs, clients_with_dst, dst_with_clients):
    print(f'Dist {dst.getsockname()} disconnected')

    client = dst_with_clients.get(dst, None)

    if client:
        client.close()
        del dst_with_clients[dst]
        del clients_with_dst[client]
        if client in clients:
            clients.remove(client)
        if client in inputs:
            inputs.remove(client)


def handle_dst_message(dst, inputs, dst_with_clients, data):
    client = dst_with_clients.get(dst, None)
    if not client:
        print('Dst sent message to unknown client', file=sys.stderr)
        dst.close()
        inputs.remove(dst)
        return
    try:
        print(f'Dst {dst.getsockname()} sent message to client {client.getsockname()}')
        client.send(data)
    except BrokenPipeError:
        print(f'Broken pipe error with {client.getsockname()}', file=sys.stderr)
    except BlockingIOError:
        print(f'Blocking IO error with {client.getsockname()}', file=sys.stderr)
    except ConnectionResetError:
        client.close()
        dst.slose()
        inputs.remove(dst)
        inputs.remove(client)


def handle_client_greeting(client, clients, inputs, data):
    if accept_new_client(client, data):
        # проверяем правильная ли версия socks пришла от клиента и метод
        print(f'Client {client.getsockname()} accepted')
        clients.append(client)
    else:
        print(f'Client {client.getsockname()} did not accept', file=sys.stderr)
        inputs.remove(client)


def handle_client_connection_request(client, dns_sock, inputs, clients, clients_with_dst, dst_with_clients,
                                     dns_id_with_clients, dns_id_with_dst_port, data):
    dst_atype = accept_client_connection(data)
    print(f'Client {client.getsockname()} sent message about dest')
    if dst_atype == ATYPE_IPV4:
        # IPV4
        dst_addr = socket.inet_ntoa(data[4:-2])
        dst_port = unpack('>H', data[8:])[0]
        dst_sock = init_dst_sock(dst_addr, dst_port)
        reply = response_on_connection_request(ST_REQUEST_GRANTED, dst_atype, dst_sock)

        try:
            client.send(reply)
        except socket.error:
            print('Error while sending response message to client', file=sys.stderr)
            client.close()
            dst_sock.close()
            inputs.remove(client)
            clients.remove(client)
            return

        clients_with_dst[client] = dst_sock
        dst_with_clients[dst_sock] = client
        inputs.append(dst_sock)

    elif dst_atype == ATYPE_DOMAINNAME:
        # DOMAIN NAME
        domain_name, dst_port = resolve_domain_name(data)
        dns_query, dns_id = create_dns_query(domain_name)
        dns_sock.send(dns_query.to_wire())
        dns_id_with_clients[dns_id] = client
        dns_id_with_dst_port[dns_id] = dst_port
        print(f'Client {client.getsockname()} waiting a domain resolve from dns server')
    else:
        # UNSUPPORTED
        print(f'Unsupported atype, message: {data}', file=sys.stderr)
        response_on_connection_request(ST_COMMAND_NOT_SUPPORTED, ATYPE_IPV4, client)
        client.close()
        clients.remove(client)
        inputs.remove(client)


def handle_client_message(client, clients, inputs, clients_with_dst, data):
    print(f'Client {client.getsockname()} sent message to dst')
    dst = clients_with_dst.get(client, None)
    if not dst:
        print(f'Error with destination of client {client.getsockname()}', file=sys.stderr)
        client.close()
        clients.remove(client)
        inputs.remove(client)
    else:
        try:
            dst.send(data)
        except socket.error:
            print('Error while sending to dst', file=sys.stderr)


def handle_message(sock, dns_sock, inputs, clients, clients_with_dst, dst_with_clients, dns_id_with_clients,
                   dns_id_with_dst_port):
    try:
        data = sock.recv(BUFFER_SIZE)
    except ConnectionResetError:
        handle_connection_reset_error(sock, inputs, clients, clients_with_dst, dst_with_clients)
        return
    except socket.error:
        print('Error while receiving data', file=sys.stderr)
        return

    if not data:

        if sock in clients:
            # Выходит клиент
            handle_client_leave(sock, clients, inputs, clients_with_dst, dst_with_clients)
        else:
            # Выходит дистант
            handle_dst_leave(sock, clients, inputs, clients_with_dst, dst_with_clients)

        sock.close()
        inputs.remove(sock)
    elif sock in dst_with_clients.keys():
        # Пришло сообщение от dst
        handle_dst_message(sock, inputs, dst_with_clients, data)

    elif sock not in clients:
        # первое сообщение от клиента после коннекта
        handle_client_greeting(sock, clients, inputs, data)

    elif sock not in clients_with_dst.keys():
        # Если клиент еще не отправил сообщение с адресом
        handle_client_connection_request(sock, dns_sock, inputs, clients, clients_with_dst, dst_with_clients,
                                         dns_id_with_clients, dns_id_with_dst_port, data)

    else:
        handle_client_message(sock, clients, inputs, clients_with_dst, data)


# Пришло сообщение От клиента дистанту


def proxy_loop(server, dns_sock, inputs):
    """Non-blocking proxy loop for accepting clients"""
    clients = []

    # сокет клиента : сокет дистанта
    clients_with_dst = {}

    # сокет дистанта : сокет клиента
    dst_with_clients = {}

    # Id днс : сокет клиента
    dns_id_with_clients = {}

    # Id днс : порт дистанта
    dns_id_with_dst_port = {}

    print(f'Server start receiving messages on address {server.getsockname()}\n')

    while not EXIT.get_status():
        try:
            reads, _, _ = select.select(inputs, [], [])
        except socket.error:
            print(f'Error select', file=sys.stderr)
            continue
        for sock in reads:

            if sock == server:
                # Клиент хочет присоединиться
                handle_new_client_connection(server, inputs)
            elif sock == dns_sock:
                # Днс прислал ответ
                handle_dns_response(dns_sock, inputs, dns_id_with_clients, dns_id_with_dst_port, clients_with_dst,
                                    dst_with_clients)
            else:
                # Кто то хочет отправить сообщение
                handle_message(sock, dns_sock, inputs, clients, clients_with_dst, dst_with_clients, dns_id_with_clients,
                               dns_id_with_dst_port)

    close_proxy_loops_sockets(inputs)


def close_proxy_loops_sockets(inputs):
    for sock in inputs:
        sock.close()


def init_dst_sock(dst_addr, dst_port):
    """init destination socket"""
    dst_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        dst_sock.connect((dst_addr, dst_port))
        dst_sock.setblocking(False)
    except socket.error:
        print(f'Error to connect dst_sock : {dst_addr} : {dst_port}', file=sys.stderr)
        return None
    return dst_sock


def init_server():
    """init server socket"""
    server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    server_sock.bind((SERVER_ADDRESS, SERVER_PORT))
    server_sock.listen(5)
    server_sock.setblocking(False)

    return server_sock


def init_dns():
    """init dns socket"""
    dns_sck = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        dns_sck.connect((DNS_ADDRESS, DNS_PORT))
    except socket.error:
        print('Error when init dns', file=sys.stderr)
        return None

    dns_sck.setblocking(False)

    return dns_sck


def handle_clients():
    """start point to run proxy loop for accepting clients"""
    server = init_server()
    dns_sock = init_dns()

    inputs = [server, dns_sock]

    signal(SIGINT, exit_handler)
    signal(SIGTERM, exit_handler)

    if not (server or dns_sock):
        return

    proxy_loop(server, dns_sock, inputs)
