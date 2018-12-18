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
# Path to a file that contains lines with the locations of files 
# containing the sentences we want for our Word2Vec model
pathsLocator = "./sentencesPaths.txt"

class MySentences():
    def __iter__(self):
        for fname in open(pathsLocator, mode='rt'):  # os.listdir(self.dirname):
            sentencesPath = fname.rstrip('\n')
            print("Grabbing sentences from: %s" % sentencesPath)
            try:
                for line in open(sentencesPath, mode='rt'):
                    lineTokens = line.rstrip('\n').split("\t")
                    words = []
                    for token in lineTokens:
                        sentence = token.split('->')
                        words.append(sentence)
                    yield words
            except Exception:
                print("Failed reading file:")
                print(sentencesPath)

sentence_generator = MySentences()

curr_words = 0
max_words = 1
for word in sentence_generator:
    print(word)
    curr_words += 1
    if curr_words >= max_words:
        break