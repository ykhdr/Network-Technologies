FROM python:latest

RUN pip install dnspython

COPY . /socks
WORKDIR /socks

CMD ["python3", "main.py"]
