dir=musagi-stqn
version=1.0.1
platform=linux_x86
debug=debug
fullversion=${version}_${platform}_${debug}
archive=${dir}_${fullversion}.tgz
[ "${debug}" = "debug" ] && Debug=Debug || Debug=Release

# Check that window title is the good one
grep "\"${dir} ${version}\"" glkit.h
if [ $? = 1 ]; then
	echo "*** Wrong window title!"
	grep "CreateGLWindow(\"${dir}" glkit.h
	echo -n "Continue anyway? [y/N] "
	read result
	[ "${result}" != "y" ] && exit
fi

# Check that NEWS is up to date
today=`date +"%Y-%m-%d"`
grep "${today}" NEWS
if [ $? = 1 ]; then
	echo "*** NEWS doesn't contain today's date!"
	echo -n "Continue anyway? [y/N] "
	read result
	[ "${result}" != "y" ] && exit
fi

if [ -e $dir ]; then
	echo "ERROR: $dir dir already exists"
	exit
fi

if [ -e $archive ]; then
	echo "ERROR: $archive already exists"
	exit
fi

mkdir $dir
cp -r documentation instruments songs skins bin/${Debug}/musagi README NEWS $dir/
tar caf $archive $dir
rm -r $dir
ls -l $archive
tar taf $archive
