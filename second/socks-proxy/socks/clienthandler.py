import socket
import select
import struct

# SERVER CONFIG
SERVER_PORT = 1080
SERVER_ADDRESS = '0.0.0.0'

# PROTOCOL PARAMETERS
VER = b'\x05'
'''Protocol version'''

# Only no auth
NAUTH = b'\0x00'
'''Number of Authentication method'''

# Only no auth possible now
AUTH = b'\x00'
'''Authentication method'''

DTYPE_IPV4 = b'\x01'
'''IPv4 address (01)'''

DTYPE_DOMAIN_NAME = b'\x03'
'''Domain name (03)'''


def client_greeting(message):
    pass


def handle_clients():
    server = init_server()
    clients = []
    destinations = []
    outputs = []
    inputs = [server]
    errors = []

    while True:
        reads, sends, err = select.select(inputs, outputs, errors)
        for sock in reads:
            if sock == server:
                # Клиент хочет присоединиться
                client, addr = server.accept()
                print(f"New connection: {addr}")

                inputs.append(client)
                clients.append(client)
            else:
                # Клиент хочет отправить сообщение
                data = sock.recv(4096)
                if not data:
                    inputs.remove(sock)

                    sock.close()


def init_server():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    server.bind((SERVER_ADDRESS, SERVER_PORT))
    server.listen(5)
    server.setblocking(False)

    return server
