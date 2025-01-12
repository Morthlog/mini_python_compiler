#! /usr/bin/env python
#A miniPython example
funcwithdef("Jogh", 10)
add()
add(4, 5, "Pop")
add(1,4,"7",9)
add2(2, 5)
doSth(2,6, "pop")

def add(x, y, z):
        return y + x + z

def doSth(num1, num2, str):
       return add(num1, num2)

def add(a, b, c):
        return None

def add(x, y, z=1):
        return None

def add(x, y):
        return x + y

def add3(x, y, z):
        return None

def add3(x, y, z=1):
        return None

def add3(x, y, z, k=1):
        return None

def funcwithdef(name, ok, university="aueb", val = 5):
       print name, " studies in ", university

def statements():
       if true:
              while false:
                     for a in b:
                            return a
print a , b
a = 55
b -= 66
c /= 77
a [a] = b
assert f , d
B()
C(88 , 3, s)

# End of statements

import math.plus
import math.div as libDiv
import math.mult as libMult, myLib as call

def comparisons():
       if not false:
              a=b
if true and false:
       a=b
if false or true:
       a=b
if not 5 > 6:
       a=b
if a != b and c < a:
       a=b
if 66 < 77 or 88 == a:
       a=b 

# End of comparisons

def value(val1 = a.func(), val2 = 8, val3= " ",val4= ' sas ',val5= None):
        return None

def expressions():
       print a
print a + 5
print a - 5
print a * 5
print a / 5
print 10 % 4
print 2 ** 3
print 1 + 2 - 3 +5 *7/7%10 - 1 ** 2
print a[8]
print A("string1")
print A.B("string2")
print 10
print len (5)
print ascii (b)
print max (6, A.func("val", "val2"), "pop", None)
print max (A.func('val', 'val2'), 'pop')
print min (3)
print (5)
print [4, "val"]

# End of expressions

def comparisonOrder():
       return None

def arithmeticOrder():
       return None

if not true:
        return a + b