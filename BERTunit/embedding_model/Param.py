"""
hyper-parameters:
"""
import os

#these 5 parameters need manual adjustment each run
LANG = ["zh", "ja", "fr"][0] #language 'zh'/'ja'/'fr'
table_setting = 1
bootstrap = 2
WITH_TRANS = False # True False
CUDA_NUM = 3 # used GPU num

if table_setting == 4 or table_setting == 0:
    bootstrap_string = "setting_" + "4_" + LANG + "_bootstrap_" + str(bootstrap)
elif table_setting == 3:
    bootstrap_string = "setting_" + "3_" + LANG + "_bootstrap_" + str(bootstrap)
elif table_setting == 1 or table_setting == 2 or table_setting == 5 or table_setting > 5:
    bootstrap_string = "setting_" + "125_" + LANG + "_bootstrap_" + str(bootstrap)

if WITH_TRANS:
    bootstrap_string = bootstrap_string + "_trans"

ENTITY_NEIGH_MAX_NUM = 50 # max sampling neighbor num of entity
ENTITY_ATTVALUE_MAX_NUM = 50 #max sampling attributeValue num of entity
KERNEL_NUM = 21
SEED_NUM = 11037
CANDIDATE_NUM = 50 # candidate number

BATCH_SIZE = 128 # train batch size128
NEG_NUM = 5 # negative sampling num
LEARNING_RATE = 5e-4 # learning rate
MARGIN = 1 # margin
EPOCH_NUM = 200 # train epoch num

INTERACTION_MODEL_SAVE_PATH = "../Save_model/interaction_model_{}en.bin".format(LANG) #interaction model save path.

#load model(base_bert_unit_model) path
BASIC_BERT_UNIT_MODEL_SAVE_PATH = "../Save_model/"
BASIC_BERT_UNIT_MODEL_SAVE_PREFIX = "DBP15K_" + bootstrap_string

LOAD_BASIC_BERT_UNIT_MODEL_EPOCH_NUM = 14
BASIC_BERT_UNIT_MODEL_OUTPUT_DIM = 300

#load data path

file_path = os.path.abspath(__file__)
project_dir = os.path.dirname(os.path.dirname(os.path.dirname(file_path)))
DATA_PATH = os.path.join(project_dir, f'datasets/DBP15k_{LANG}_en/')
DBP = True#True  False

#candidata_save_path
TRAIN_CANDIDATES_PATH = DATA_PATH + 'train_candidates.pkl'
TEST_CANDIDATES_PATH = DATA_PATH + 'test_candidates.pkl'

#entity embedding and attributeValue embedding save path.
if not WITH_TRANS:
    ENT_EMB_PATH = DATA_PATH + str(bootstrap_string) + "_entity_emb.csv"
else:
    ENT_EMB_PATH = DATA_PATH + str(bootstrap_string) + "_trans_entity_emb.csv"
EMB_ENT_NAME_PATH = DATA_PATH + LANG + "_emb_entity_names"
ATTRIBUTEVALUE_EMB_PATH = DATA_PATH + bootstrap_string + "attribute_value_embedding.pkl"
if(DBP):
    ATTRIBUTEVALUE_LIST_PATH = DATA_PATH + bootstrap_string + "_attribute_value_list.pkl" #1-1 match to attributeValue embedding.
    ATTRIBUTEVALUE_SIM_PATH = DATA_PATH + bootstrap_string + "_attribute_value_sim.pkl"
else:
    ATTRIBUTEVALUE_LIST_PATH = DATA_PATH + "attribute_value_list.pkl" #1-1 match to attributeValue embedding.
    ATTRIBUTEVALUE_SIM_PATH = DATA_PATH + "attribute_value_sim.pkl"

#(candidate) entity_pairs save path.
ENT_PAIRS_PATH = DATA_PATH + 'ent_pairs.pkl' #[(e1,ea),(e1,eb)...]

#interaction feature save filepath name
NEIGHBORVIEW_SIMILARITY_FEATURE_PATH = DATA_PATH + 'neighbor_view_similarity_feature.pkl' #1-1 match to entity_pairs
ATTRIBUTEVIEW_SIMILARITY_FEATURE_PATH = DATA_PATH + 'attribute_similarity_feature.pkl' #1-1 match to entity_pairs
DESVIEW_SIMILARITY_FEATURE_PATH = DATA_PATH + 'des_view_similarity_feature.pkl' #1-1 match to entity_pairs
