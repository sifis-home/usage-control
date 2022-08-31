# To be sure that UCSService has started
adb shell am start -n com.example.ucsintent/.UCSActivity
timeout 10
adb shell input keyevent 4

$j = 0
$i = 0
$single_test_repetitions = 30
$data = @("xmls/experiments/onePIP/tryAccess1.xml",
"xmls/experiments/onePIP/tryAccess2.xml",
"xmls/experiments/onePIP/tryAccess5.xml",
"xmls/experiments/onePIP/tryAccess10.xml",
"xmls/experiments/onePIP/tryAccess20.xml",
"xmls/experiments/onePIP/tryAccess30.xml",
"xmls/experiments/onePIP/tryAccess40.xml",
"xmls/experiments/onePIP/tryAccess50.xml",
"xmls/experiments/onePIP/tryAccess100.xml",
"xmls/experiments/onePIP/tryAccess200.xml",
"xmls/experiments/onePIP/tryAccess500.xml",
"xmls/experiments/onePIP/tryAccess1000.xml",
"xmls/experiments/onePIP/tryAccess2000.xml",
"xmls/experiments/onePIP/tryAccess5000.xml",
"xmls/experiments/onePIP/tryAccess10000.xml")


while($j -lt $data.Length){
    echo "Finished test for  $($data[$j])"
    while ($i -lt $single_test_repetitions){
        adb shell am start -n com.example.hookedapp/.ExperimentActivity --es "accessType" "tryAccess" --es "policyPath" $data[$j]
        timeout 5
        adb shell input keyevent 4
        $i++
    }
    $i = 0
    $j++
}

