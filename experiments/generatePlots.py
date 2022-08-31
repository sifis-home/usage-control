import numpy as np
import scipy.stats
import matplotlib.pyplot as plt

plt.rcParams.update({'errorbar.capsize': 3})
def mean_confidence_interval(data, confidence=0.95):
    a = 1.0 * np.array(data)
    n = len(a)
    m, se = np.mean(a), scipy.stats.sem(a)
    h = se * scipy.stats.t.ppf((1 + confidence) / 2., n-1)
    return m, m-h, m+h


def generate_axes(input_filename, x):
    file1 = open(input_filename, 'r')
    lines = file1.readlines()

    graph_values = {}
    for line in lines:
        splitted_line = line.strip().split("\t")    
        if splitted_line[1] == "ENTER" or splitted_line[1] == "REEVALUATION_Deny":
            diff_time = int(splitted_line[0])
        else:
            diff_time = int(splitted_line[0]) - diff_time
            if splitted_line[2] not in graph_values.keys():
                graph_values[splitted_line[2]] = []
            graph_values[splitted_line[2]].append(diff_time)
            

    y = []
    yerr = [[], []]
    for k,v in graph_values.items():
        print(k, v)
        
        ci = mean_confidence_interval(v)
        print(f"CI: {ci}")    
        y.append(ci[0])
        yerr[0].append(abs(ci[1] - ci[0]))
        yerr[1].append(abs(ci[2] - ci[0]))
        
    return x, y, yerr

x1, y1, yerr1 = generate_axes("data/experiment_try_access_many_pips.log", [1, 2, 5, 10, 20, 30, 40, 50])
x2, y2, yerr2 = generate_axes("data/experiment_try_access_one_pip.log", [1, 2, 5, 10, 20, 30, 40, 50])
x3, y3, yerr3 = generate_axes("data/experiment_start_access_one_pip.log", [1, 2, 5, 10, 20, 30, 40, 50])
x4, y4, yerr4 = generate_axes("data/experiment_try_access_one_pip_extended.log", [1, 2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000])
xbase, ybase, yerrbase = generate_axes("data/experiment_try_access_base.log", [1])
x128tr, y128tr, yerr128tr = generate_axes("data/experiment_try_access_128.log", [1, 2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000])
x256tr, y256tr, yerr256tr = generate_axes("data/experiment_try_access_256.log", [1, 2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000])
x384tr, y384tr, yerr384tr = generate_axes("data/experiment_try_access_384.log", [1, 2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000])
x512tr, y512tr, yerr512tr = generate_axes("data/experiment_try_access_512.log", [1, 2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000])
x128st, y128st, yerr128st = generate_axes("data/experiment_start_access_128.log", [1, 2, 5, 10, 20, 30])
x256st, y256st, yerr256st = generate_axes("data/experiment_start_access_256.log", [1, 2, 5, 10, 20, 30, 40, 50])
x384st, y384st, yerr384st = generate_axes("data/experiment_start_access_384.log", [1, 2, 5, 10, 20, 30, 40, 50])
x512st, y512st, yerr512st = generate_axes("data/experiment_start_access_512.log", [1, 2, 5, 10, 20, 30, 40, 50])

x128tr2, y128tr2, yerr128tr2 = generate_axes("data/experiment_try_access_128_3072.log", [1, 2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000])
x256tr2, y256tr2, yerr256tr2 = generate_axes("data/experiment_try_access_256_3072.log", [1, 2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000])
x512tr2, y512tr2, yerr512tr2 = generate_axes("data/experiment_try_access_512.log", [1, 2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000])
x1024tr2, y1024tr2, yerr1024tr2 = generate_axes("data/experiment_try_access_1024_3072.log", [1, 2, 5, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000])

x128st2, y128st2, yerr128st2 = generate_axes("data/experiment_start_access_128_3072.log", [1, 2, 5, 10, 20, 30, 40, 50])
x256st2, y256st2, yerr256st2 = generate_axes("data/experiment_start_access_256_3072.log", [1, 2, 5, 10, 20, 30, 40, 50])
x512st2, y512st2, yerr512st2 = generate_axes("data/experiment_start_access_512.log", [1, 2, 5, 10, 20, 30, 40, 50])
x1024st2, y1024st2, yerr1024st2 = generate_axes("data/experiment_start_access_1024_3072.log", [1, 2, 5, 10, 20, 30, 40, 50])


# plotting
plt.title("Time for TryAccess with Different PIPs")
plt.xlabel("Number of Attributes in policy")
plt.ylabel("Time for TryAccess [ms]")
plt.plot(x1, y1, color ="green")
plt.errorbar(x1, y1, yerr=yerr1, fmt='o',
             ecolor = 'black',color='green', label="50 PIPs")
plt.axhline(y = ybase[0], color = 'r', linestyle = 'dashed', label="Without UCS") 
plt.legend()
plt.savefig('plots/tryAccessTimeDifferentPIPS.png')
plt.close()

plt.title("Time for TryAccess with One PIP")
plt.xlabel("Number of Attributes in policy")
plt.ylabel("Time for TryAccess [ms]")
plt.plot(x2, y2, color ="green")
plt.errorbar(x2, y2, yerr=yerr2, fmt='o',
             ecolor = 'black',color='green', label="1 PIP")
plt.axhline(y = ybase[0], color = 'r', linestyle = 'dashed', label="Without UCS") 
plt.legend()
plt.savefig('plots/tryAccessTimeOnePIP.png')
plt.close()

plt.title("Inconsistency Time by varying number of attributes")
plt.xlabel("Number of Attributes in policy")
plt.ylabel("Inconsistency Time [ms]")
plt.plot(x3, y3, color ="green")
plt.errorbar(x3, y3, yerr=yerr3, fmt='o',
             ecolor = 'black',color='green')
plt.savefig('plots/startAccessInconsistencyTimeOnePIP.png')
plt.close()


plt.title("Time for TryAccess")
plt.xlabel("Number of Attributes in policy")
plt.ylabel("Time for TryAccess [ms]")
plt.plot(x4, y4, color ="green")
plt.errorbar(x4, y4, yerr=yerr4, fmt='o',
             ecolor = 'black',color='green', label="1 PIP")
plt.axhline(y = ybase[0], color = 'r', linestyle = 'dashed', label="Without UCS") 
plt.legend()
plt.savefig('plots/tryAccessTimeOnePIPExtended.png')
plt.close()

plt.title("Time for TryAccess")
plt.xlabel("Number of Attributes in policy")
plt.ylabel("Time for TryAccess [ms]")
plt.plot(x1, y1, color ="green", label="50 PIPs")
plt.errorbar(x1, y1, yerr=yerr1, fmt='o',
             ecolor = 'black',color='green')
plt.plot(x2, y2, color ="blue", label="1 PIP")
plt.errorbar(x2, y2, yerr=yerr2, fmt='o',
             ecolor = 'black',color='blue')
plt.axhline(y = ybase[0], color = 'r', linestyle = 'dashed', label="Without UCS") 
plt.legend()
plt.savefig('plots/tryAccess.png')
plt.close()

plt.title("Time for TryAccess by VM heap/RAM Size")
plt.xlabel("Number of Attributes in policy")
plt.ylabel("Time for TryAccess [ms]")
plt.plot(x128tr, y128tr, color ="green", label="128 MB / 768 MB")
plt.errorbar(x128tr, y128tr, yerr=yerr128tr, fmt='o',
             ecolor = 'black',color='green')
plt.plot(x256tr, y256tr, color ="blue", label="256 MB / 1518 MB")
plt.errorbar(x256tr, y256tr, yerr=yerr256tr, fmt='o',
             ecolor = 'black',color='blue')
plt.plot(x384tr, y384tr, color ="red", label="384 MB / 2274 MB")
plt.errorbar(x384tr, y384tr, yerr=yerr384tr, fmt='o',
             ecolor = 'black',color='red')
plt.plot(x512tr, y512tr, color ="yellow", label="512 MB / 3072 MB")
plt.errorbar(x512tr, y512tr, yerr=yerr512tr, fmt='o',
             ecolor = 'black',color='yellow')
plt.axhline(y = ybase[0], color = 'r', linestyle = 'dashed', label="Without UCS") 
plt.legend()
plt.savefig('plots/tryAccessExperiment.png')
plt.close()

plt.title("Inconsistency Time by VM heap/RAM Size")
plt.xlabel("Number of Attributes in policy")
plt.ylabel("Inconsistency Time [ms]")
plt.plot(x128st, y128st, color ="green", label="128 MB / 768 MB")
plt.errorbar(x128st, y128st, yerr=yerr128st, fmt='o',
             ecolor = 'black',color='green')
plt.plot(x256st, y256st, color ="blue", label="256 MB / 1518 MB")
plt.errorbar(x256st, y256st, yerr=yerr256st, fmt='o',
             ecolor = 'black',color='blue')
plt.plot(x384st, y384st, color ="red", label="384 MB / 2274 MB")
plt.errorbar(x384st, y384st, yerr=yerr384st, fmt='o',
             ecolor = 'black',color='red')
plt.plot(x512st, y512st, color ="yellow", label="512 MB / 3072 MB")
plt.errorbar(x512st, y512st, yerr=yerr512st, fmt='o',
             ecolor = 'black',color='yellow')
plt.axhline(y = ybase[0], color = 'r', linestyle = 'dashed', label="Without UCS") 
plt.legend()
plt.savefig('plots/startAccessExperiment.png')
plt.close()


plt.title("Time for TryAccess by VM heap (3084MB RAM Size)")
plt.xlabel("Number of Attributes in policy")
plt.ylabel("Time for TryAccess [ms]")
plt.plot(x128tr2, y128tr2, color ="green", label="128 MB")
plt.errorbar(x128tr2, y128tr2, yerr=yerr128tr2, fmt='o',
             ecolor = 'black',color='green')
plt.plot(x256tr2, y256tr2, color ="blue", label="256 MB")
plt.errorbar(x256tr2, y256tr2, yerr=yerr256tr2, fmt='o',
             ecolor = 'black',color='blue')
plt.plot(x512tr, y512tr, color ="yellow", label="512 MB")
plt.errorbar(x512tr, y512tr, yerr=yerr512tr, fmt='o',
             ecolor = 'black',color='yellow')
plt.plot(x1024tr2, y1024tr2, color ="red", label="1024 MB")
plt.errorbar(x1024tr2, y1024tr2, yerr=yerr1024tr2, fmt='o',
             ecolor = 'black',color='red')
plt.axhline(y = ybase[0], color = 'r', linestyle = 'dashed', label="Without UCS") 
plt.legend()
plt.savefig('plots/tryAccessExperimentByVMHeap.png')
plt.close()

plt.title("Inconsistency Time by VM heap (3084MB RAM Size)")
plt.xlabel("Number of Attributes in policy")
plt.ylabel("Inconsistency Time [ms]")
plt.plot(x128st2, y128st2, color ="green", label="128 MB")
plt.errorbar(x128st2, y128st2, yerr=yerr128st2, fmt='o',
             ecolor = 'black',color='green')
plt.plot(x256st2, y256st2, color ="blue", label="256 MB")
plt.errorbar(x256st2, y256st2, yerr=yerr256st2, fmt='o',
             ecolor = 'black',color='blue')
plt.plot(x512st, y512st, color ="yellow", label="512 MB")
plt.errorbar(x512st, y512st, yerr=yerr512st, fmt='o',
             ecolor = 'black',color='yellow')
plt.plot(x1024st2, y1024st2, color ="red", label="1024 MB")
plt.errorbar(x1024st2, y1024st2, yerr=yerr1024st2, fmt='o',
             ecolor = 'black',color='red')
plt.axhline(y = ybase[0], color = 'r', linestyle = 'dashed', label="Without UCS") 
plt.legend()
plt.savefig('plots/startAccessExperimentByVMHeap.png')
