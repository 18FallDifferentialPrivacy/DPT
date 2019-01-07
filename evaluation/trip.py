import sys
import random
import math
import scipy.stats
import numpy as np


def readfile1(filename):
    lines = open(filename).readlines()
    count = 0
    database = []
    trajectory = []
    for i in range(1,len(lines)):
        string = lines[i]
        if string[0]=="#":
            count += 1
            database.append(trajectory)
            trajectory = []
        if string[0] == ">":
            line = string.split(":")
            data_all = line[1]
            data = data_all.split(";")
            for j in range(1,len(data)-2):
                detail = data[j].split(",")
                point = [int(detail[0]), int(detail[1]), int(detail[2])]
                trajectory.append(point)
    database.append(trajectory)
    return database


def readfile2(filename):
    lines = open(filename).readlines()
    database = []
    for i in range(len(lines)):
        if i % 2 == 1:
            trajectory = []
            line = lines[i].split(">0:")
            data_all = line[1].split(";")
            for j in range(len(data_all)-1):
                detail = data_all[j].split(",")
                point = [float(detail[0]), float(detail[1])]
                trajectory.append(point)
            database.append(trajectory)
    return database


def trip(database1, database2, system):
    temp_base = []
    for i in range(len(database1)):
        start_point = database1[i][0]
        end_point = database1[i][-1]
        if start_point[2] == system and end_point[2] == system:
            temp_base.append(i)
    print(temp_base)
    distribution = 0
    for i in range(1000):
        distribution += each_trip(temp_base, database1, database2, system)
    return distribution/1000


def each_trip(temp_base, database1, database2, system):
    # region = []
    width = 340/(system+1)
    height = 400/(system+1)
    a1 = random.randint(0, len(temp_base)-1)
    b1 = random.randint(0, len(temp_base)-1)
    a = database1[temp_base[a1]][0][0]
    b = database1[temp_base[b1]][0][1]
    # for p in range(a-1, a+2):
    #     for q in range(b-1, b+2):
    #         region.append([p, q])
    current_index = []
    for i in range(len(temp_base)):
        index = temp_base[i]
        start_point = database1[index][0]
        if (a-1) <= start_point[0] <= (a+1) and (b-1) <= start_point[0] <= (b+1):
            current_index.append(index)
    dis1 = []
    dis2 = []
    for i in range(14):
        dis1.append(0)
        dis2.append(0)
    for i in range(len(current_index)):
        current1 = database1[current_index[i]]
        head1 = current1[0]
        tail1 = current1[-1]
        distance1 = math.sqrt((head1[0]-tail1[0])**2+(head1[1]-tail1[1])**2)
        dis1[int(distance1/4000)] += 1
        current2 = database2[current_index[i]]
        head2 = current2[0]
        tail2 = current2[-1]
        distance2 = math.sqrt((head2[0] - tail2[0]) ** 2 + (head2[1] - tail2[1]) ** 2)
        dis2[int(distance2 / 4000)] += 1
    num1 = 0
    num2 = 0
    for i in range(len(dis1)):
        num1 += dis1[i]
    for i in range(len(dis2)):
        num2 += dis2[i]
    if num1 != 0:
        for i in range(len(dis1)):
            dis1[i] = dis1[i]/num1
    if num2 != 0:
        for i in range(len(dis2)):
            dis2[i] = dis2[i]/num2
    if num1 == 0 and num2 == 0:
        return 0
    p = np.array(dis1)
    q = np.array(dis2)
    return JS_divergence(p, q)


def JS_divergence(p,q):
    M=(p+q)/2
    return 0.5*scipy.stats.entropy(p, M)+0.5*scipy.stats.entropy(q, M)


def main():
    filename1 = sys.argv[1]
    filename2 = sys.argv[2]
    database1 = readfile1(filename1)
    database2 = readfile2(filename2)
    result = trip(database1, database2, 0)
    print(result)


if __name__ == '__main__':
    main()
