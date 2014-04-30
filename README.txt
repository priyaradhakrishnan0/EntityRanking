README
-------------------------------------------
This software is the implementation of the paper "Predicting Previous Version of a Product Entity" published at SIGIR'14.  This paper describes a two step approach to solve the problem of predicting the predecessor version of a given query product version.
A) parsing the product title, and (B) predicting the predecessor version given all candidates.

Step A:
1. input dataset = "camera.zip" in http://tinyurl.com/lclapy8

2. Preprocess - create feature vector for CRF

java -jar FeatureVector.jar train/test/label camera/EVALUATION $file debug/none eval/none time/none

//TRAIN
java -jar FeatureVector.jar train camera $file none none none
//TEST
java -jar FeatureVector.jar test camera $file none none none
//LABEL TRAIN_TEST
java -jar FeatureVector.jar label camera $file none none none

3. Run - predict labels
TRAIN
mallet-2.0.7$ java -cp "/home/priya/Desktop/Tools/mallet-2.0.7/class://home/priya/Desktop/Tools/mallet-2.0.7/lib/mallet-deps.jar" cc.mallet.fst.SimpleTagger --train true --model-file camera_train camera_sample
TEST
mallet-2.0.7$ java -cp "/home/priya/Desktop/Tools/mallet-2.0.7/class://home/priya/Desktop/Tools/mallet-2.0.7/lib/mallet-deps.jar" cc.mallet.fst.SimpleTagger --include-input true  --model-file camera_train camera_sample

4. Results
//Compute Precision, Recall and F measure  
java -jar ResultAnalyzer.jar camera_label_t6_1 camera_label_t6_2

Step B:
1. input dataset = “goldenData” in http://tinyurl.com/lclapy8

2. Preprocess - fetch earliest review date feature
./ageRecorder.sh

3. Predict - create feature vector for Prediction
./predictPredecessor.sh

4. Evaluate Precision, Recall and F measure  using Naive Bayes Classifier
-------------------------------------------------------------------------------------
Kindly cite the paper if you are using the software 







