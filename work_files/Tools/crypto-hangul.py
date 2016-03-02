# encoding: utf-8

import sys

reload(sys)
sys.setdefaultencoding('utf8')


def encrypt(ch):
	base = 0xAC00
	
	if ord(ch) == 0x20:
		return '모'

	if ord(ch) < 0xAC00 or ord(ch) > 0xD7A3:
		return ''

	crypt_initials = [
	'거',
	'거거',
	'로',
	'자',
	'자자',
	'가',
	'라',
	'러',
	'러러',
	'구',
	'구구',
	'', # ㅇ
	'저',
	'저저',
	'마',
	'조',
	'머',
	'고',
	'주'
	]
	
	crypt_medians = [
	'그',
	'지',
	'디',
	'지',
	'리',
	'르',
	'기',
	'으',
	'므',
	'므그',
	'므지',
	'므미',
	'이',
	'드',
	'드리',
	'드르',
	'드미',
	'비',
	'즈',
	'브',
	'미'
	]
	
	crypt_finals = [
	'', # x
	'더', # ㄱ
	'더더',
	'더오',
	'어',
	'어무',
	'어아',
	'루',
	'보',
	'보더',
	'보버',
	'보다',
	'보오',
	'보부',
	'보우',
	'보아',
	'버',
	'다',
	'다오',
	'오',
	'바',
	'도',
	'무',
	'두',
	'두',
	'부',
	'우',
	'아'
	]
	
	key = ord(ch) - base
	size_f = len(crypt_finals)
	size_m = len(crypt_medians)

	initial = key / (size_m * size_f)
	median = (key / size_f) % size_m
	final = key % size_f

	return crypt_initials[initial] + crypt_medians[median] + crypt_finals[final]


plainmsg = u'''동해 물과 백두산이 마르고 닳도록 하느님이 보우하사 우리나라 만세
남산 위에 저 소나무 철갑을 두른 듯 바람서리 불변함은 우리 기상일세
가을 하늘 공활한데 높고 구름 없이 밝은 달은 우리 가슴 일편단심일세
무궁화 삼천리 화려강산 대한 사람 대한으로 길이 보전하세'''
cryptmsg = []

width = 24

for i in range(len(plainmsg)):
	crypted = unicode(encrypt(plainmsg[i]))

	for j in range(len(crypted)):
		cryptmsg.append(crypted[j])

for i in range(len(cryptmsg)):
	if i % width == 0 and i > 0:
		sys.stdout.write("\n")

	sys.stdout.write(cryptmsg[i])

sys.stdout.write("\n")
