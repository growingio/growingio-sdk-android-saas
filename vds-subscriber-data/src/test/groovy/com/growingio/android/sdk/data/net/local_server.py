#!/usr/bin/env python3

import socket

HOST = ''
PORT = 8080

if __name__ == '__main__':
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((HOST, PORT))
    server.listen(1)
    conn, addr = server.accept()
    print("connected by: ", addr)
    while True:
        data = conn.recv(1024*1024*100)
        if not data:
            print("断开连接")
            break
        print("收到消息: ", str(len(data)))
