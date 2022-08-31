# To be sure that UCSService has started
adb shell am start -n com.example.ucsintent/.UCSActivity
timeout 10
adb shell input keyevent 4

$j = 0
$i = 0
$single_test_repetitions = 30
$data = @("xmls/experiments/manyPIPS/tryAccess1.xml",
"xmls/experiments/manyPIPS/tryAccess2.xml",
"xmls/experiments/manyPIPS/tryAccess5.xml",
"xmls/experiments/manyPIPS/tryAccess10.xml",
"xmls/experiments/manyPIPS/tryAccess20.xml",
"xmls/experiments/manyPIPS/tryAccess30.xml",
"xmls/experiments/manyPIPS/tryAccess40.xml",
"xmls/experiments/manyPIPS/tryAccess50.xml")


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

