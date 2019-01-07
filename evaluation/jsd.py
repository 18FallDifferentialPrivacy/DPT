import scipy.stats
import numpy as np
import sys
import math


def read_file(filename, percentage):
    lines = open(filename).readlines()
    for i in range(len(lines)):
        trajectory = []
        data_all = lines[i]
        data = data_all.split(";")
        for j in range(len(data)-1):
            trajectory.append(get_point(data[j]))
        cal_per(trajectory, percentage)
    return percentage


def get_point(data):
    detail = data.split(",")
    point = [float(detail[0]), float(detail[1])]
    return point


def cal_per(trajectory, percentage):
    for i in range(0, len(trajectory)):
        pre_point = trajectory[i]
        next_point = []
        max_dis = 0
        for j in range(0, len(trajectory)):
            if i != j:
                next_point = trajectory[j]
                distance = math.sqrt((pre_point[0]-next_point[0])**2+(pre_point[1]-next_point[1])**2)
                if distance > max_dis:
                    max_dis = distance
        temp = max_dis/800
        if temp < 25:
            percentage[int(temp)] += 1


def JS_divergence(p,q):
    M=(p+q)/2
    return 0.5*scipy.stats.entropy(p, M)+0.5*scipy.stats.entropy(q, M)


def main():
    total1 = []
    total2 = []
    for i in range(25):
        total1.append(0)
        total2.append(0)
    total1 = read_file(sys.argv[1], total1)
    total2 = read_file(sys.argv[2], total2)
    num1 = 0
    num2 = 0
    per1 = []
    per2 = []
    for i in range(len(total1)):
        num1 += total1[i]
    for i in range(len(total2)):
        num2 += total2[i]
    for i in range(len(total1)):
        per1.append(total1[i]/num1)
    for i in range(len(total2)):
        per2.append(total2[i] / num2)
    print(per1)
    print(per2)
    result1 = np.array(per1)
    result2 = np.array(per2)
    print(JS_divergence(result1,result2))


if __name__ == '__main__':
    main()
