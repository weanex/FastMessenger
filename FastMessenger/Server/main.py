import socket
import threading
import sqlite3
import time
from datetime import datetime

class SimpleChatServer:
    def __init__(self, host='0.0.0.0', port=5555):
        self.host = host
        self.port = port
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.clients = {}
        self.lock = threading.Lock()
        self.setup_database()
    
    def setup_database(self):
        """Создание простой базы данных для чата"""
        self.conn = sqlite3.connect('chat.db', check_same_thread=False)
        self.cursor = self.conn.cursor()
        
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                message TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        self.conn.commit()
        print("База данных готова")
    
    def get_connected_count(self):
        """Получить количество подключенных пользователей"""
        with self.lock:
            return len(self.clients)
    
    def get_usernames_list(self):
        """Получить список имен подключенных пользователей"""
        with self.lock:
            return [username for username, _ in self.clients.values()]
    
    def update_user_count(self):
        """Отправить обновленное количество пользователей всем клиентам"""
        count = self.get_connected_count()
        users_list = self.get_usernames_list()
        
        # Формируем сообщение для обновления
        update_msg = f"USERS_COUNT:{count}:{','.join(users_list)}"
        self.broadcast(update_msg)
    
    def save_message(self, username, message):
        """Сохранить сообщение в базу"""
        try:
            self.cursor.execute(
                "INSERT INTO messages (username, message) VALUES (?, ?)",
                (username, message)
            )
            self.conn.commit()
            return True
        except Exception as e:
            print(f"Ошибка сохранения: {e}")
            return False
    
    def load_history(self, limit=50):
        """Загрузить историю сообщений"""
        try:
            self.cursor.execute(
                "SELECT username, message, timestamp FROM messages ORDER BY timestamp DESC LIMIT ?",
                (limit,)
            )
            rows = self.cursor.fetchall()
            
            history = []
            for row in reversed(rows):
                time_str = datetime.strptime(row[2], '%Y-%m-%d %H:%M:%S').strftime('%H:%M')
                history.append(f"[{time_str}] {row[0]}: {row[1]}")
            
            return "|".join(history)
        except Exception as e:
            print(f"Ошибка загрузки истории: {e}")
            return ""
    
    def broadcast(self, message, exclude_client=None):
        """Отправить сообщение всем клиентам"""
        with self.lock:
            for client, (username, _) in list(self.clients.items()):
                if client != exclude_client:
                    try:
                        client.send(message.encode('utf-8'))
                    except:
                        self.remove_client(client)
    
    def remove_client(self, client):
        """Удалить клиента"""
        with self.lock:
            if client in self.clients:
                username, _ = self.clients[client]
                del self.clients[client]
                
                # Уведомляем об отключении
                leave_msg = f"[{time.strftime('%H:%M')}] {username} покинул чат"
                self.broadcast(leave_msg)
                
                # Обновляем счетчик пользователей
                self.update_user_count()
                
                print(f"{username} отключился")
    
    def handle_client(self, client, address):
        """Обработка клиента"""
        username = None
        
        try:
            # Получаем имя пользователя
            data = client.recv(1024).decode('utf-8')
            if not data or not data.startswith("USERNAME:"):
                client.close()
                return
            
            username = data.split(":", 1)[1]
            
            with self.lock:
                self.clients[client] = (username, address)
            
            print(f"{username} подключился ({address[0]}:{address[1]})")
            
            # Отправляем историю
            history = self.load_history(20)
            if history:
                client.send(f"HISTORY:{history}".encode('utf-8'))
            
            # Отправляем начальное количество пользователей
            count = self.get_connected_count()
            users_list = self.get_usernames_list()
            client.send(f"USERS_COUNT:{count}:{','.join(users_list)}".encode('utf-8'))
            
            # Уведомляем о новом пользователе
            join_msg = f"[{time.strftime('%H:%M')}] {username} присоединился к чату"
            self.broadcast(join_msg, client)
            
            # Обновляем счетчик пользователей для всех
            self.update_user_count()
            
            # Основной цикл
            while True:
                data = client.recv(1024).decode('utf-8')
                if not data:
                    break
                
                if data == "DISCONNECT":
                    break
                elif data.startswith("MESSAGE:"):
                    message = data.split(":", 1)[1]
                    
                    # Сохраняем сообщение
                    self.save_message(username, message)
                    
                    # Формируем и рассылаем сообщение
                    timestamp = time.strftime('%H:%M')
                    broadcast_msg = f"[{timestamp}] {username}: {message}"
                    self.broadcast(broadcast_msg)
        
        except ConnectionResetError:
            pass
        except Exception as e:
            print(f"Ошибка с клиентом {username}: {e}")
        finally:
            if username:
                self.remove_client(client)
            client.close()
    
    def start(self):
        """Запуск сервера"""
        try:
            self.server.bind((self.host, self.port))
            self.server.listen()
            print(f"Сервер запущен на {self.host}:{self.port}")
            print("Ожидание подключений...")
            
            while True:
                client, address = self.server.accept()
                thread = threading.Thread(target=self.handle_client, args=(client, address))
                thread.daemon = True
                thread.start()
        
        except KeyboardInterrupt:
            print("\nОстановка сервера...")
        finally:
            self.server.close()
            self.conn.close()

if __name__ == "__main__":
    server = SimpleChatServer()
    server.start()