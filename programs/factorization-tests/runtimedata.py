#! /usr/bin/env python3

import sys

import matplotlib.pyplot as plt

def findMinMax(elements):
    minimum = elements[0]
    maximum = elements[0]

    for i in range(1, len(elements)):
        if (elements[i] < minimum):
            minimum = elements[i]
        if (elements[i] > maximum):
            maximum = elements[i]
    return (minimum, maximum)


STATE_TITLE = 0
STATE_DATA = 1

inputfile = sys.argv[1]

labels   = []
minTimes = []
maxTimes = []


with open(inputfile) as f:
    title = ""
    times = []
    
    state = STATE_TITLE

    for line in [line.rstrip() for line in f]:
        if len(line) == 0:
            state = STATE_TITLE

            if len(times) > 0:
                minTime, maxTime = findMinMax(times)
                minTimes.append(minTime)
                maxTimes.append(maxTime)
                labels.append(title)

                times = []
                title = ""

        elif state == STATE_TITLE:
            state = STATE_DATA

            title = line

        elif state == STATE_DATA:
            times.append(int(line))


plt.rcdefaults()
fig, ax = plt.subplots()

y_pos = range(len(labels))
times = [max_t - min_t for (max_t, min_t) in zip(maxTimes, minTimes)]

ax.barh(y_pos, times, left=minTimes)

ax.set_yticks([])

ax.set_xlabel('Time (ticks)')
ax.set_title('Execution times ' + inputfile)
ax.set_xlim(left=0)

for rect, label in zip(ax.patches, labels):
    ax.text(rect.get_x() + rect.get_width() + 2, rect.get_y(), label)

plt.show()

