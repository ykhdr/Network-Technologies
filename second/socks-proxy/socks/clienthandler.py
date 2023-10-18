import socket
import select
a
# SERVER CONFIG
SERVER_PORT = 1080
SERVER_ADDRESS = '0.0.0.0'

# PROTOCOL PARAMETERS
VER = b'\x05'
'''Protocol version'''

# Only no auth possible now
M_NOAUTH = b'\x00'
'''Authentication method'''

M_NOTAVAILABLE = b'\xff'
'''Requested methods are not available'''

DTYPE_IPV4 = b'\x01'
'''IPv4 address (01)'''

DTYPE_DOMAIN_NAME = b'\x03'
'''Domain name (03)'''

# UTILS
BUFFER_SIZE = 4 * 1024


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

    return True if method == M_NOTAVAILABLE else False


def client_connection_request(client, data):
    # TODO проверить на правильность сообщения о запросе подключения и в случае успеха совершить подключение и
    #  вернуть сокет
    pass


def handle_clients():
    server = init_server()
    clients = []
    clients_with_destination = {}
    inputs = [server]

    while True:
        reads, _, _ = select.select(inputs, [], [])
        for sock in reads:
            if sock == server:
                # Клиент хочет присоединиться
                client, addr = server.accept()
                print(f"New connection: {addr}")

                inputs.append(client)
            else:
                # Клиент хочет отправить сообщение
                data = sock.recv(BUFFER_SIZE)
                if not data:
                    print(f'Client {sock.getpeername()} disconnected')
                    inputs.remove(sock)
                    sock.close()

                if sock not in clients:
                    if accept_new_client(sock, data):
                        print(f'Client {sock.getpeername()} accepted')
                        clients.append(sock)
                    else:
                        print(f'Client {sock.getpeername()} did not accept')
                        inputs.remove(sock)
                elif sock not in clients_with_destination.keys():
                    client_connection_request(sock, data)

                else:
                    dest = clients_with_destination.get(sock)
                    if not dest:
                        print(f'Error with destination of client {sock.getppername()}')
                        # TODO сообщить клиенту об ошибке и закрыть соединение
                    else:
                        dest.sendall(data)


def init_server():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    server.bind((SERVER_ADDRESS, SERVER_PORT))
    server.listen(5)
    server.setblocking(False)

    return server
