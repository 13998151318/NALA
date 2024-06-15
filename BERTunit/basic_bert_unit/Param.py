print("In params:")
LANG = 'fr' #language 'zh'/'ja'/'fr'

CUDA_NUM = 2 # used GPU num
MODEL_INPUT_DIM  = 768
MODEL_OUTPUT_DIM = 300 # dimension of basic bert unit output embedding
RANDOM_DIVIDE_ILL = False #if is True: get train/test_ILLs by random divide all entity ILLs, else: get train/test ILLs from file.
TRAIN_ILL_RATE = 0.3 # (only work when RANDOM_DIVIDE_ILL == True) training data rate. Example: train ILL number: 15000 * 0.3 = 4500.

SEED_NUM = 11037

ACTUALLY_DO_TRAINING = True#False
EPOCH_NUM = 15 #training epoch num
WITH_TRANS = True#True

NEAREST_SAMPLE_NUM = 128
CANDIDATE_GENERATOR_BATCH_SIZE = 128

TOPK = 50
NEG_NUM = 2 # negative sample num
MARGIN = 3 # margin
LEARNING_RATE = 1e-5 # learning rate
TRAIN_BATCH_SIZE = 24 #24
TEST_BATCH_SIZE = 128 #128

DES_LIMIT_LENGTH = 128 # max length of description/name.
USE_DESC = False

excel_num = 411
DATA_PATH = r"../data/dbp15k/{}_en_2/".format(LANG)  #data path
DES_DICT_PATH = r"../data/dbp15k/2016-10-des_dict" #description data path
MODEL_SAVE_PATH = "../Save_model/"                 #model save path
if WITH_TRANS:
    MODEL_SAVE_PREFIX = "trans_" + str(excel_num) + "_DBP15K_{}en".format(LANG)
else:
    MODEL_SAVE_PREFIX = str(excel_num) + "_DBP15K_{}en".format(LANG)

import os
if not os.path.exists(MODEL_SAVE_PATH):
    os.makedirs(MODEL_SAVE_PATH)


print("NEG_NUM:",NEG_NUM)
print("MARGIN:",MARGIN)
print("LEARNING RATE:",LEARNING_RATE)
print("TRAIN_BATCH_SIZE:",TRAIN_BATCH_SIZE)
print("TEST_BATCH_SIZE",TEST_BATCH_SIZE)
print("DES_LIMIT_LENGTH:",DES_LIMIT_LENGTH)
print("RANDOM_DIVIDE_ILL:",RANDOM_DIVIDE_ILL)
print("")
print("")
