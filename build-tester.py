print "Enter the things you want to build, in order."
print "Type 'DONE' to get the results."
print ""

def eq(s1, s2):
	le = min(len(s1), len(s2))
	return s1[:le].lower() == s2[:le].lower()

builds = ["TREE", "TANK", "GARDENER", "LUMBERJACK", "SOLDIER", "SCOUT"]

wanted = []
s = raw_input()
while not eq(s, "DONE"):
	choice = None
	for x in builds:
		if eq(s, x):
			choice = x
			break
	if choice == None:
		print "Couldn't match %s with something that can be built" % s
	else:
		wanted.append(choice)
	s = raw_input()

money = 300
trees = []
gardeners = []
round = 1

def advance():
	global money, trees, gardeners, round
	if money < 200:
		money += 2 - (money / 100.0)
	round += 1
	for t in trees:
		if round - t >= 80:
			money += 1
	for i in range(len(gardeners)):
		gardeners[i] -= 1

def takeGardener():
	global gardeners
	if len(gardeners) == 0:
		return False
	gardeners.sort()
	if gardeners[0] > 0:
		return False
	gardeners[0] = 10
	return True

def waitFor(amount, gardenerNeeded):
	global money, round, gardeners
	while round < 3000 and money < amount:
		advance()
	if gardenerNeeded:
		while not takeGardener():
			advance()
	money -= amount


def build(x):
	global gardeners, trees, round
	if x == "TREE":
		waitFor(50, True)
		trees.append(round)
	elif x == "TANK":
		waitFor(300, True)
	elif x == "LUMBERJACK" or x == "SOLDIER":
		waitFor(100, True)
	elif x == "SCOUT":
		waitFor(80, True)
	elif x == "GARDENER":
		waitFor(100, False)
		gardeners.append(1)
	else:
		assert False
	print "%s built on round %d" % (x, round)

for x in wanted:
	build(x)
