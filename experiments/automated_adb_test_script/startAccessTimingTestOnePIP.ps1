# To be sure that UCSService has started
adb shell am start -n com.example.ucsintent/.UCSActivity
timeout 10
adb shell input keyevent 4
adb shell su -c sh /data/write_0.sh

$j = 0
$i = 0
$single_test_repetitions = 30
$data = @("xmls/experiments/onePIP/startAccess1.xml",
"xmls/experiments/onePIP/startAccess2.xml",
"xmls/experiments/onePIP/startAccess5.xml",
"xmls/experiments/onePIP/startAccess10.xml",
"xmls/experiments/onePIP/startAccess20.xml",
"xmls/experiments/onePIP/startAccess30.xml",
"xmls/experiments/onePIP/startAccess40.xml",
"xmls/experiments/onePIP/startAccess50.xml",
"xmls/experiments/onePIP/startAccess100.xml",
"xmls/experiments/onePIP/startAccess200.xml",
"xmls/experiments/onePIP/startAccess500.xml",
"xmls/experiments/onePIP/startAccess1000.xml",
"xmls/experiments/onePIP/startAccess2000.xml",
"xmls/experiments/onePIP/startAccess5000.xml",
"xmls/experiments/onePIP/startAccess10000.xml")


while($j -lt $data.Length){
    echo "Finished test for  $($data[$j])"
    while ($i -lt $single_test_repetitions){
        adb shell am start -n com.example.hookedapp/.ExperimentActivity --es "accessType" "startAccess" --es "policyPath" $data[$j]
        timeout 5
        adb shell su -c sh /data/write_10.sh
        timeout 10
        adb shell input keyevent 4
        adb shell su -c sh /data/write_0.sh
        $i++
    }
    $i = 0
    $j++
}

