#!/usr/bin/env python
import os, sys, subprocess
import unittest, drillbit, drillbit_sdk
import optparse, types

drillbitDir = os.path.abspath(os.path.dirname(sys._getframe(0).f_code.co_filename))
mobileSdkDir = drillbit.extract_mobilesdk()

sdkTestsDir = os.path.join(drillbitDir, "sdk_tests")
sys.path.append(sdkTestsDir)

def main():
	parser = optparse.OptionParser()
	parser.add_option("-t", "--tests", dest="tests", default=None,
		help="Tests to run (default: all)")
	(options, args) = parser.parse_args()

	runner = unittest.TextTestRunner(verbosity=2)
	loader = unittest.TestLoader()

	if options.tests == None:
		import android, iphone
		androidSuite = loader.loadTestsFromModule(android)
		iphoneSuite = loader.loadTestsFromModule(iphone)
		allSuites = unittest.TestSuite([androidSuite, iphoneSuite])
		runner.run(allSuites)
	else:
		tests = options.tests.split(",")
		suite = unittest.TestSuite()
		for test in tests:
			t = __import__(test)
			if isinstance(t, unittest.TestCase):
				suite.addTest(t)
			elif type(t) == types.MethodType:
				suite.addTest(t.im_class(t.__name__))
			elif type(t) == types.ClassType:
				suite.addTests(loader.loadTestsFromClass(t))
			elif test in sys.modules:
				suite.addTests(loader.loadTestsFromModule(sys.modules[test]))
			elif type(t) == types.ModuleType:
				suite.addTests(loader.loadTestsFromModule(t))
		runner.run(suite)

if __name__ == "__main__":
	main()
