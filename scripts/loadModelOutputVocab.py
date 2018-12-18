'''
Created on Feb 16, 2016
Updated November 2018
@author: petar
@author: kris
'''
# import modules; set up logging
from gensim.models import Word2Vec
import logging, os, sys, gzip
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', filename='word2vec.out', level=logging.INFO)


to_load = 'DB2Vec_sg_500_5_5_15_2_500'
vocab_path = './test_vocab.txt'

#load model
print("Loading model from: ",to_load)
model = Word2Vec.load(to_load)

#output vocabulary
print("Outputting vocab to: ",vocab_path)
outFile = open(vocab_path, "w")
for word, vocab_obj in model.wv.vocab.items():
    #print("Word: ", word)
    #print("vocab_obj: ", vocab_obj)
    #print(model.wv.word_vec(word))
    outFile.write("%s" % word)
    for item in model.wv.word_vec(word):
        outFile.write("\t%s" % item)
    #outFile.write("\t%s" % value)
    outFile.write("\n")
outFile.close()
print("Finished outputting vocabulary")
