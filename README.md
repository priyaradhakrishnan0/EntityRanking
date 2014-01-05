<pre>
EntityRanking
=============
README
-------------------------------------------
1. input dataset = amazon web crawl
src - amazon.sh

2. Preprocess - create feature vector

java -jar FeatureVector.jar train/test/label camera/EVALUATION $file debug/none eval/none time/none

//TRAIN
java -jar FeatureVector.jar train camera $file none none none
//TEST
java -jar FeatureVector.jar test camera $file none none none
//LABEL TRAIN_TEST
java -jar FeatureVector.jar label camera $file none none none

//EVALUATION test 
java -jar FeatureVector.jar test EVALUATION $file none none none
//EVALUATION label
python  evalLabeller.py

//Time variant features
 java -jar FeatureVector.jar train camera $file none none time


3. Run - predict labels
TRAIN
mallet-2.0.7$ java -cp "/home/priya/Desktop/Tools/mallet-2.0.7/class://home/priya/Desktop/Tools/mallet-2.0.7/lib/mallet-deps.jar" cc.mallet.fst.SimpleTagger
  --train true --model-file camera_train camera_sample
TEST
mallet-2.0.7$ java -cp "/home/priya/Desktop/Tools/mallet-2.0.7/class://home/priya/Desktop/Tools/mallet-2.0.7/lib/mallet-deps.jar" cc.mallet.fst.SimpleTagger
--include-input true  --model-file camera_train camera_sample

4. Results
java -jar ResultAnalyzer.jar <true label file> <predicted label file>
//java -jar ResultAnalyzer.jar camera_label_t6_1 camera_label_t6_2

5. Evaluation 
src
dataset =  getNewModel.sh
labelling = evalLabeller.py
</pre>






