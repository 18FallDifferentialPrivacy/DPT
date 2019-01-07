import sys


def readfile(filename1, filename2):
    lines = open(filename1).readlines()
    write_file = open(filename2, "w")
    for i in range(1000):
        if i % 2 == 1:
            line = lines[i].split(">0:")
            for j in range(len(line)):
                write_file.write(line[j])
    write_file.close()


def main():
    file1 = sys.argv[1]
    file2 = sys.argv[2]
    readfile(file1, file2)


if __name__ == '__main__':
    main()
