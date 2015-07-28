#!/usr/bin/python
# -*- coding: utf-8 -*-

# For DiMe server queries
import requests
import socket
import json

#For storing lists
import pickle 

#
import sys

#
import numpy as np

#
from scipy import sparse
import scipy.sparse.linalg

#For LDA (Latent dirichlet allocation)
import gensim
from gensim import corpora, models, similarities, interfaces

#For rapid keyword extraction
import rake

#for comparing words 
#Learn tfidf model from the document term matrix
import difflib

#
import os


#Update data
def update_data(srvurl, username, password):
	### IMPORT DATA from DiMe #######	
	print "Importing data from DiMe!!"

	#------------------------------------------------------------------------------

	server_url = str(srvurl)
	server_username = str(username)
	server_password = str(password)

	#------------------------------------------------------------------------------

	# ping server (not needed, but fun to do :-)
	r = requests.post(server_url + '/ping')

	if r.status_code != requests.codes.ok:
		print('No connection to DiMe server!')
		sys.exit(1)

	r = requests.get(server_url + '/data/informationelement',
	                 headers={'content-type': 'application/json'},
	                 auth=(server_username, server_password),
	                 timeout=10)
	 
	data = r.json()
	
	#
	f = open('json_data.txt','w')	
	json.dump(data, f)



# Update dictionary
def update_dictionary():

	print "Updating dictionary!"
	f = open('json_data.txt','r')
	data = json.load(f)

	#Create list of documents
	ndocuments = len(data)
	print ndocuments
	documentlist = []
	for i in range(ndocuments):
		documentlist.append(data[i]['plainTextContent'])



	#####################
	# Update dictionary from words occurring in 'documentlist' ################
	#####################

	# Read list of forbidden words #
	s1 = open('stop-words/stop-words_english_1_en.txt','r')
	s2 = open('stop-words/stop-words_english_2_en.txt','r')
	s3 = open('stop-words/stop-words_english_3_en.txt','r')
	s4 = open('stop-words/stop-words_finnish_1_fi.txt','r')
	s5 = open('stop-words/stop-words_finnish_2_fi.txt','r')

	sstr1=s1.read()
	sstr2=s2.read()
	sstr3=s3.read()
	sstr4=s4.read()
	sstr5=s5.read()

	#
	sstr  = sstr1 + sstr2 + sstr3 + sstr4 + sstr5
	stoplist = set(sstr.split())

	#
	wordlist = []
	#Take all words
	for doc in documentlist:
		#pass
		for word in doc.lower().split():
			#Check if the string consist of alphabetic characters only
			if word.isalpha() and (word not in stoplist):
				wordlist.append(word)

	#Create dictionary
	dictionary = corpora.Dictionary([wordlist])

	#Save dictionary for future use
	dictionary.save('/tmp/tmpdict.dict')
	#dictionaryvalues = dictionary.values()



#Update document term matrix
def update_doctm():

	print "Updating document term matrix!"

	#Import dictionary
	dictionary = corpora.Dictionary.load('/tmp/tmpdict.dict')

	#Import data
	f = open('json_data.txt','r')
	data = json.load(f)

	#Create list of documents
	ndocuments = len(data)
	documentlist = []
	for i in range(ndocuments):
		documentlist.append(data[i]['plainTextContent'])

	#Create document term matrix 'doctm'
	doctm = []
	for i in range(len(documentlist)):
		test_vec_dum = dictionary.doc2bow(documentlist[i].lower().split())
		doctm.append(test_vec_dum)


	#Store the data of doctm
	f = open('doctm.data','w')
	pickle.dump(doctm, f)

def update_X():

	print "Update X tfidf matrix!"

	#Import dictionary
	dictionary = corpora.Dictionary.load('/tmp/tmpdict.dict')

	f = open('doctm.data','r')
	doctm = pickle.load(f)
	#print 'rows doctm', len(doctm)

	#Learn tfidf model from the document term matrix
	tfidf = models.TfidfModel(doctm, normalize = True)

	#Make tfidf-list from doctm using tfidf-model
	doc_tfidf_list = []
	for i in range(len(doctm)):
	    doc_tfidf_dum = tfidf[doctm[i]]
	    doc_tfidf_list.append(doc_tfidf_dum)


	#Create tfidf numpy array
	#Initialize tfidf-matrix (numpy-array)
	nwords = len(dictionary)
	ndocuments = len(doctm)

	tfidf_array = np.zeros((nwords,ndocuments))
	#
	for nvec, tfidf_vec in enumerate(doc_tfidf_list):
	    #print nvec
	    for wi in range(len(tfidf_vec)):
	        wid, score = tfidf_vec[wi]
	        #print wid, score
	        tfidf_array[wid, nvec] = score

	tfidf_array = tfidf_array.transpose()
	print 'tfidf ', tfidf_array.shape

	#Normalize rows of tfidf_array
	for i in range(tfidf_array.shape[0]):
		dumvec = tfidf_array[i][:]
		sumvec = np.sum(dumvec,axis=0)
		if sumvec > 0.0:
			dumvec = dumvec/float(sumvec)
		tfidf_array[i][:] = dumvec
		#print 'X row sum: ', np.sum(tfidf_array[i][:], axis = 0)

	#Let's simplify notation
	X      = tfidf_array
	#print X.shape

	#Save Xt matrix into a binary NumPy file for further use
	np.save('X.npy',X)


def update_Xt_and_docindlist(docindlist):

	#print docindlist 
	#Import X matrix needed in LinRel
	X = np.load('X.npy')

	#Save docindlist 
	if os.path.isfile('docindlist.list'):
		f = open('docindlist.list','r')
		docindlistprev = pickle.load(f)
		#Merge new and old list of indices
		docindlist = list(set(docindlist + docindlistprev))
		f = open('docindlist.list','w')
		pickle.dump(docindlist,f)
	else:
		#Save docindlist (the list of indices of suggested documents)		
		f = open('docindlist.list','w')
		pickle.dump(docindlist,f)
	#Save list of indices of omitted documents
	if os.path.isfile('docindlist.list'):
		#docindlisttot = np.array([range(X.shape[0])])
		docindlisttot = range(X.shape[0])
		omittedindlist= np.delete(docindlisttot,docindlist,0)
		omittedindlist= omittedindlist.tolist()
		#print len(omittedindlist)
		#print len(docindlist)
		#print len(docindlisttot)

		#Save list of omitted docs
		f = open('omittedindlist.npy','w')
		pickle.dump(omittedindlist,f)

	if os.path.isfile('Xt.npy'):
		print "Updating Xt tfidf matrix!"
		#Xt = np.load('Xt.npy')

		#print 'docindlist:', docindlist
		Xt = X[docindlist][:]
		#newrows = X[docindlist][:] 
		#Xt = np.vstack([Xt, newrows])
	else:
		print "Initializing Xt"
		Xt = np.array( [ X[0][:] ] )

	#Save Xt matrix into a binary NumPy file for further use
	np.save('Xt.npy',Xt)


	return Xt


#Update similarity vector
def update_docsim_model():
	#Import dictionary 
	dictionary = corpora.Dictionary.load('/tmp/tmpdict.dict')

	#Import document term matrix
	f = open('doctm.data','r')
	doctm = pickle.load(f)

	#Build index
	index = similarities.docsim.Similarity('/tmp/similarityvec',doctm, num_features = len(dictionary), num_best = 7)
	index.save('/tmp/similarityvec')


#
def create_topic_model_and_doctid(numoftopics):

	print "Create topic model!"

	#Import data
	f = open('json_data.txt','r')
	data = json.load(f)

	#Import dictionary
	dictionary = corpora.Dictionary.load('/tmp/tmpdict.dict')

	#Import document term matrix
	f = open('doctm.data','r')
	doctm = pickle.load(f)

	#Number of documents in database
	ndocuments = len(doctm)

	#Compute cluster distributions (topics)
	#ldamodel = models.LdaModel(doctm, num_topics = numoftopics, id2word = dictionary, passes=10, iterations=200)
	ldamodel = models.LdaModel(doctm, num_topics = numoftopics, id2word = dictionary, passes=10, iterations=20)

	#Save 'ldamodel' for future use
	ldamodel.save('/tmp/tmpldamodel')

    #Compute topic ids for documents
	dtid = []
	for i in range(len(doctm)):
		dumtidv = []
		dumtpv  = []
		doc_lda = ldamodel[doctm[i]]
		for j in range(len(doc_lda)):
			dumtidv.append(doc_lda[j][0])
			dumtpv.append(doc_lda[j][1])
		dtid.append(dumtidv[np.argmax(dumtpv)])
	dtid = np.array(dtid)
	np.save('doctid.npy',dtid)


	#Print words in topics
	# for i in range(numoftopics):
	# 	#Print most probable words in each cluster
	# 	print 'topic ' + str(i)
	# 	prkw = ldamodel.show_topic(i, topn=10)
	# 	for j in range(len(prkw)):
	# 		print prkw[j][0], prkw[j][1]

	
	#Compute doc. ind list of each topic
	topicdocindlist = []
	for i in range(numoftopics):
		#print np.where(dtid == i)[0]
		topicdocindlist.append(np.where(dtid==i)[0])

	#Store the data of doctm
	f = open('topicdocindlist.list','w')
	pickle.dump(topicdocindlist, f)


#
def update_topic_model_and_doctid():

	print "Updating topic model!"

	#Import data
	f = open('json_data.txt','r')
	data = json.load(f)

	#Import dictionary
	dictionary = corpora.Dictionary.load('/tmp/tmpdict.dict')

	#Import lda model
	ldamodel = models.LdaModel.load('/tmp/tmpldamodel')

	#Import document term matrix
	f = open('doctm.data','r')
	doctm = pickle.load(f)

	#Import topic document index list (for computing the number of topics )
	f = open('topicdocindlist.list','r')
	topicdocindlist = pickle.load(f)
	numoftopics = len(topicdocindlist)

	#Number of documents in database
	ndocuments = len(doctm)

	#Compute cluster distributions (topics)
	#ldamodel = models.LdaModel(doctm, num_topics = numoftopics, id2word = dictionary, passes=10, iterations=200)
	ldamodel.update(doctm)

	#Save 'ldamodel' for future use
	ldamodel.save('/tmp/tmpldamodel')

    #Compute topic ids for documents
	dtid = []
	for i in range(len(doctm)):
		dumtidv = []
		dumtpv  = []
		doc_lda = ldamodel[doctm[i]]
		for j in range(len(doc_lda)):
			dumtidv.append(doc_lda[j][0])
			dumtpv.append(doc_lda[j][1])
		dtid.append(dumtidv[np.argmax(dumtpv)])
	dtid = np.array(dtid)
	np.save('doctid.npy',dtid)

	#Compute doc. ind list of each topic
	topicdocindlist = []
	for i in range(numoftopics):
		#print np.where(dtid == i)[0]
		topicdocindlist.append(np.where(dtid==i)[0])

	#Store the data of doctm
	f = open('topicdocindlist.list','w')
	pickle.dump(topicdocindlist, f)


#
def update_topic_keywords():

	#Import data
	f = open('json_data.txt','r')
	data = json.load(f)

	#Import lda model
	ldamodel = models.LdaModel.load('/tmp/tmpldamodel')

	#Import document term matrix
	f = open('doctm.data','r')
	doctm = pickle.load(f)

	#Import topic document index list
	f = open('topicdocindlist.list','r')
	topicdocindlist = pickle.load(f)
	numoftopics = len(topicdocindlist)
#
	doctid = np.load('doctid.npy')
	#Compute three keywords of each topic
	#Initialize rake object
	#each word has at least 4 characters,
	#each phrase at most 1 words,
	#each keyword appears in the text at least 10 times
	rake_object = rake.Rake("SmartStoplist.txt", 4, 5, 5)
	textcontent = ''
	#clusterdocinds = [0,200,110,150]
	topickeywords = []
	for topicdocinds in topicdocindlist:
		#print topicdocinds
		for i in topicdocinds:
			teststr      = data[i]["plainTextContent"]
			textcontent  = textcontent + ' ' + data[i]["plainTextContent"]
		topickeyword = rake_object.run(textcontent)
		#print topickeyword

		if len(topickeyword) > 0:
			topickeywords.append(topickeyword[0])
		else:
			topickeywords.append('test')
		textcontent = ''

	for i in range(len(topickeywords)):
		print topickeywords[i]
		#print keywords[0:2]

	#Show top topics
	#print ldamodel.top_topics(doctm, num_words=10)


#Updates LinRel matrix
#(Not recommended)
def update_A_slow():
	print "Updating A"
	#
	X  = np.load('X.npy')
	Xt = np.load('Xt.npy')
	XtT= np.transpose(Xt)

	#Convert into a scipy sparse matrix
	sX  = sparse.csc_matrix(X)
	sXt = sparse.csc_matrix(Xt)
	sXtT= sparse.csc_matrix(XtT)

	print sX.shape
	print sXt.shape

	#Regularization parameter
	mu = 10.5

	print "Compute XtTXt"
	sXtTXt = sXtT*sXt
	print sXtTXt.shape
	nr, nc = sXtTXt.shape
	print nc
	sI   = sparse.identity(nc)
	#sI  = sparse.csr_matrix(I)
	sM  = sXtTXt + mu*sI
	print "inv of sM"
	sMinv = sparse.linalg.inv(sM)	
	print sMinv.shape
	sM2 = sMinv*sXtT
	print "Compute A"
	sA     = sX*sM2
	#Convert to full form
	A     = sA.toarray()
	A     = np.array(A)
	print 'A shape:', A.shape
	
	#Save A matrix into a binary NumPy file for further use
	np.save('A.npy',A)


#Updates LinRel matrix
def update_A(X, Xt, y):

	print "Updating A"
	#
	#X  = np.load('X.npy')
	#Xt = np.load('Xt.npy')
	#Compute estimation of weight vector 
	w = estimate_w(Xt,y)
	w = np.array([w])
	w = w.transpose()

	yT   = y.transpose()
	norm_y= np.linalg.norm(yT)
	if norm_y > 0.0:
		yT   = yT/(norm_y*norm_y)

	#Update A
	Atilde= w*yT
	A     = np.dot(X,Atilde)
	np.save('A.npy', A)
	#update_Xt_and_docindlist(docinds)

	return A

 
#Compute Tikhonov regularized solution for y=Xt*w (using scipy function lsqr)
#I.e. compute estimation of user model
def estimate_w(Xt,y):
	#
	mu = 1.5
	w = scipy.sparse.linalg.lsqr(Xt,y, damp=mu)[0]
	return w





