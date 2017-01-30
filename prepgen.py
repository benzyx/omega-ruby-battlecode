def gen(n):
	if n == 0:
		return [0]
	else:
		a = gen(n - 1)
		ret = []
		for x in a:
			ret.append(x)
		for x in a:
			ret.append(x + 6.283185307179586476925286766559 / 2 ** n)
		return ret

for x in gen(4):
	print "new Direction(%.10ff)," % x
