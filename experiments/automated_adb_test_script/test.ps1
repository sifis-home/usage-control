
adb shell su -c sh /data/write_0.sh
timeout 5
adb shell am start -n com.example.hookedapp/.ExperimentActivity --es "accessType" "startAccess" --es "policyPath" "xmls/experiments/onePIP/startAccess500.xml"
timeout 5
adb shell su -c sh /data/write_10.sh
