newline = $"\n"
while read line
do
echo $line
brand=`echo $line | cut -d' ' -f1`
product=`echo $line | cut -d' ' -f2`
goldPred=`echo $line | cut -d' ' -f3`
goldSucc=`echo $line | cut -d' ' -f4`
java -jar PreviosVersionPredictor $brand $product $goldPred $goldSucc 
echo $newline
done <  goldenData
