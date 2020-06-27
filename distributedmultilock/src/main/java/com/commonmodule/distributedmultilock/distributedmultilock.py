import socket
import thread
import threading
import time

from redis.sentinel import Sentinel

redis_host = [('192.168.1.1', 26379), ('192.168.1.2', 26379), ('192.168.1.3', 26379)]


class DistributedMultiLock:
    lua_lock_script = '''if redis.call('ZCARD', KEYS[1]) >= tonumber(ARGV[2])
                        then
                            return 0
                        else
                            if not redis.call('ZRANK', KEYS[1], KEYS[2])
                            then
                                return redis.call('ZADD', KEYS[1], ARGV[1], KEYS[2])
                            else
                                return 1
                            end
                        end'''

    lua_unlock_script = '''if redis.call('ZRANK', KEYS[1], KEYS[2])
                           then
                                return redis.call('ZREM', KEYS[1], KEYS[2])
                           else
                                return 1
                           end'''

    lua_watchdog_script = '''if redis.call('ZRANK', KEYS[1], KEYS[2])
                              then
                                  redis.call('ZADD', KEYS[1], ARGV[1], KEYS[2])
                                  redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[2])
                                  redis.call('EXPIRE', KEYS[1], ARGV[3])
                                  return 1
                              else
                                  return 0
                              end'''

    def __init__(self, key, maxconcurrent):
        self.key = 'distributedmultilock-' + key
        self.value = ''
        self.maxconcurrent = maxconcurrent
        self.interval = 30
        self.__locked = False
        self.redisObj = Sentinel(redis_host, socket_timeout=200.0).master_for('search-cache', socket_timeout=200.0)

    def __enter__(self):
        while not self.trylock():
            print 'get lock fail , try again in 30 seconds...'
            time.sleep(30)
        print 'get lock'
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.unlock()
        print 'unlock success'

    def trylock(self):
        if not self.__locked:
            ip = socket.gethostbyname(socket.getfqdn(socket.gethostname()))
            pid = threading.currentThread().ident
            self.value = ip + '#' + str(pid)
            timestamp = int(time.time())
            result = self.redisObj.eval(self.lua_lock_script, 2, self.key, self.value, timestamp, self.maxconcurrent)
            if int(result) > 0:
                print 'get lock key=', self.key, 'value=', self.value
                self.__locked = True
                thread.start_new_thread(self.watchdog, ())
                return True
            else:
                return False
        else:
            return True

    def unlock(self):
        self.__locked = False
        self.redisObj.eval(self.lua_unlock_script, 2, self.key, self.value)
        print 'unlock key=', self.key, 'value=', self.value

    def watchdog(self):
        while 1:
            if self.__locked:
                lastactivetime = int(time.time())
                inactivitetime = lastactivetime - 15 * 60
                self.redisObj.eval(self.lua_watchdog_script, 2, self.key, self.value, lastactivetime,
                                   inactivitetime, 30 * 60)
                print 'feeding....key=', self.key, 'value=', self.value
                time.sleep(self.interval)
            else:
                break


if __name__ == '__main__':
    with DistributedMultiLock("myapp", 2):
        while 1:
            print 'process..'
            time.sleep(5)