import os
print("In params:")

# these 6 parameters need manual adjustment in each run
LANG = ["zh", "ja", "fr"][0] #language 'zh'/'ja'/'fr'
table_setting = 1
bootstrap = 2
WITH_TRANS = False # True False
USE_DESC = False
CUDA_NUM = 2 # used GPU num

if table_setting == 4 or table_setting == 0:
    bootstrap_string = "setting_" + "4_" + LANG + "_bootstrap_" + str(bootstrap)
elif table_setting == 3:
    bootstrap_string = "setting_" + "3_" + LANG + "_bootstrap_" + str(bootstrap)
elif table_setting == 1 or table_setting == 2 or table_setting == 5 or table_setting > 5:
    bootstrap_string = "setting_" + "125_" + LANG + "_bootstrap_" + str(bootstrap)

if WITH_TRANS:
    bootstrap_string = bootstrap_string + "_trans"

ACTUALLY_DO_TRAINING = True
if table_setting == 3:
    if bootstrap == 1:
        ACTUALLY_DO_TRAINING = False

MODEL_INPUT_DIM  = 768
MODEL_OUTPUT_DIM = 300 # dimension of basic bert unit output embedding
RANDOM_DIVIDE_ILL = False #if is True: get train/test_ILLs by random divide all entity ILLs, else: get train/test ILLs from file.
TRAIN_ILL_RATE = 0.3 # (only work when RANDOM_DIVIDE_ILL == True) training data rate. Example: train ILL number: 15000 * 0.3 = 4500.

SEED_NUM = 11037

EPOCH_NUM = 15 #training epoch num
NEAREST_SAMPLE_NUM = 128
CANDIDATE_GENERATOR_BATCH_SIZE = 128

TOPK = 50
NEG_NUM = 2 # negative sample num
MARGIN = 3 # margin
LEARNING_RATE = 1e-5 # learning rate
TRAIN_BATCH_SIZE = 24 #24
TEST_BATCH_SIZE = 128 #128

DES_LIMIT_LENGTH = 128 # max length of description/name.

use_original_sup_pairs = False

file_path = os.path.abspath(__file__)
project_dir = os.path.dirname(os.path.dirname(os.path.dirname(file_path)))
#print('project_dir=',project_dir)
#print("cur work dir = ", os.getcwd())
DATA_PATH = os.path.join(project_dir, f'datasets/DBP15k_{LANG}_en/')
DES_DICT_PATH = os.path.join(project_dir, f'2016-10-des_dict')

#DATA_PATH = r"../data/dbp15k/{}_en_full/".format(LANG)  #data path
#DES_DICT_PATH = r"../data/dbp15k/2016-10-des_dict" #description data path
MODEL_SAVE_PATH = "../Save_model/"                 #model save path
MODEL_SAVE_PREFIX = "DBP15K_" + bootstrap_string


    

if not os.path.exists(MODEL_SAVE_PATH):
    os.makedirs(MODEL_SAVE_PATH)

print("DATA_PATH:",DATA_PATH)
print("NEG_NUM:",NEG_NUM)
print("MARGIN:",MARGIN)
print("LEARNING RATE:",LEARNING_RATE)
print("TRAIN_BATCH_SIZE:",TRAIN_BATCH_SIZE)
print("TEST_BATCH_SIZE",TEST_BATCH_SIZE)
print("DES_LIMIT_LENGTH:",DES_LIMIT_LENGTH)
print("RANDOM_DIVIDE_ILL:",RANDOM_DIVIDE_ILL)
print("")
print("")
