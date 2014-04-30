for file in `cat camera_doldset2`
do
java -jar PredecessorVersionFeatureVector.jar $file 
done

