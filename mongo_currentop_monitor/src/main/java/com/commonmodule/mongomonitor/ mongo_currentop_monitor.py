import signal
import sys
from datetime import datetime
import time
from collections import Counter
from collections import defaultdict
from pymongo import MongoClient

client = MongoClient("mongodb://xx:xxx@127.0.0.1:27023/admin")
db = client.admin

dic = defaultdict(list)
global_dic = defaultdict(list)
global_uuid_list = list()


def run():
    signal.signal(signal.SIGINT, quit)
    signal.signal(signal.SIGTERM, quit)
    while True:
        current_ops = db.current_op()
        inprogs = current_ops['inprog']

        for item in inprogs:
            if 'client' in item:
                # dic[item['ns'] + " - " + item['op']].append(item['client'])
                opid = item['opid']
                if opid not in global_uuid_list:
                    global_dic[item['ns'] + " - " + item['op'] + " - " + item.get('planSummary', "")].append(
                        item['client'])
                    global_uuid_list.append(opid)

        # printdic(dic)
        # print
        # dic.clear()
        time.sleep(0.1)


def printdic(innerdic):
    print 'time:', datetime.now()
    for ns, iplist in innerdic.items():
        print ns
        cnt_total = Counter(iplist)
        for ip in cnt_total.most_common():
            print '\t', ip[0], ip[1]


def printrep(innerdic):
    for ns, iplist in innerdic.items():
        cnt_total = Counter(iplist)
        cmd = 0
        for ip in cnt_total.most_common():
            cmd += int(ip[1])
        print ns, cmd


def quit(signum, frame):
    print ''
    print ''
    print ''
    printdic(global_dic)
    print ''
    print ''
    print '-----------------------------'
    printrep(global_dic)
    sys.exit()


if __name__ == '__main__':
    run()