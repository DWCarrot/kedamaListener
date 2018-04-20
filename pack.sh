#/bin/sh

filelist=`ls -c jsonRecord/*`
echo "[" >> jsonRecord.json
for file in $filelist
do
	cat $file >> jsonRecord.json
done
echo "]" >> jsonRecord.json

filelist=`ls -c csvRecord/*`
echo "timestamp,time,online" >> csvRecord.csv
for file in $filelist
do
	cat $file >> csvRecord.csv
done

time=`date --iso-8601`
read -r -p "package name [data-$time.tar.gz]: " input
if [ ! $input ];then
	input="data-$time.tar.gz"
fi
echo ""
tar -zcvf $input jsonRecord.json csvRecord.csv jsonRecord/ csvRecord/ logs/
rm jsonRecord.json
rm csvRecord.csv
echo ""
du -h ./*
echo ""

read -r -p "Delete old logs? [Y/n] " input
case $input in
[yY][eE][sS]|[yY])
	filelist=`ls -c logs/main-*.log`
	for file in $filelist
	do
		echo "rm $file"
		rm $file
	done
	echo ""
	;;
[nN][oO]|[nN])
 	;;
*)
	echo "Invalid input..."
	;;
esac

read -r -p "Delete old json records? [Y/n] " input
case $input in
[yY][eE][sS]|[yY])
	filelist=`ls -c jsonRecord/record-*.json`
	for file in $filelist
	do
		echo "rm $file"
		rm $file
	done
	echo ""
	;;
[nN][oO]|[nN])
 	;;
*)
	echo "Invalid input..."
	;;
esac

read -r -p "Delete old csv records? [Y/n] " input
case $input in
[yY][eE][sS]|[yY])
	filelist=`ls -c csvRecord/record-*.csv`
	for file in $filelist
	do
		echo "rm $file"
		rm $file
	done
	echo ""
	;;
[nN][oO]|[nN])
 	;;
*)
	echo "Invalid input..."
	;;
esac
