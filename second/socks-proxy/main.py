import dns.message
import dns.query
import socket
import select
import random

from socks.clienthandler import init_dns


def resolve_dns_with_select(domain, dns_server):
    unique_id = random.randint(0, 65535)  # Генерируем случайный идентификатор из диапазона 0-65535

    # Создаем DNS-запрос
    request = dns.message.make_query(domain, dns.rdatatype.A)
    request.id = unique_id

    # Создаем сокет для отправки DNS-запросов клиентам
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    client_socket.bind(('127.0.0.1', 0))  # Привязываем к локальному адресу

    # Создаем сокет для чтения DNS-ответов от сервера
    server_socket = init_dns() # Привязываем к локальному адресу

    try:
        server_socket.sendall(request.to_wire())

        while True:
            readable, _, _ = select.select([server_socket], [], [], 5)  # Ожидаем ответ в течение 5 секунд

            if server_socket in readable:
                response_data, _ = server_socket.recvfrom(1024)
                response = dns.message.from_wire(response_data)
                process_dns_response(response, unique_id)
                break
            else:
                print("DNS query timed out")
                break
    finally:
        client_socket.close()
        server_socket.close()

def process_dns_response(response, unique_id):
    for answer in response.answer:
        if answer.rdtype == dns.rdatatype.A:
            print(f"Resolved IP for ID {unique_id}: {answer[0].address}")

if __name__ == '__main__':
    dns_server = '8.8.8.8'  # Замените на нужный DNS-сервер
    domain = 'ya.ru'  # Замените на желаемый домен

    resolve_dns_with_select(domain, dns_server)
