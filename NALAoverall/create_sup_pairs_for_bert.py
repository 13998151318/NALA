import os
from utils import * 
import functools
import random
output_path = "/home/2022xuch/PNAL/PNALoverall/output"
#lang = "zh"
#lang_path = "/zh_en"
#prefix1 = "http://" + lang + ".dbpedia.org/resource/"
#prefix2 = "http://dbpedia.org/resource/"
fold_path = "/DBP15K_FULL_zh_en_0728_141349"
excel_num = 539
full_fold_path = output_path + fold_path

#dataset_path = "/home/2022xuch/paris/datasets/DBP15k_full_" + lang + "_en_2"
run = 19 #21

abbreviate_dict = {"http://dbpedia.org/resource/":"dbp_en:","http://dbpedia.org/property/":"dbp_en_prop:","http://zh.dbpedia.org/resource/":"dbp_zh:","http://zh.dbpedia.org/property/":"dbp_zh_prop:"
                   ,"http://fr.dbpedia.org/resource/":"dbp_fr:","http://fr.dbpedia.org/property/":"dbp_fr_prop:"
                   ,"http://ja.dbpedia.org/resource/":"dbp_ja:","http://ja.dbpedia.org/property/":"dbp_ja_prop:"
                   ,"http://www.w3.org/2001/XMLSchema#":"xsd:","http://dbpedia.org/datatype/":"dbp_type:"
                   ,"http://xmlns.com/foaf/0.1/":"foaf:"
                   ,"http://purl.org/dc/elements/1.1/":"purl:"}

def abbreviate(str1, r=0, attribute=0):
    for full in abbreviate_dict.keys():
        if str1.startswith(full):
            str1 = abbreviate_dict[full] + str1[len(full):]
            break
    if r:
        if attribute and not str1.startswith("attr_"):
            str1 = "attr_" + str1
        elif not attribute and not str1.startswith("rel_"):
            str1 = "rel_" + str1
    return str1

def un_abbreviate(str1, r=0, attribute=0):
    rdict = dict(zip(abbreviate_dict.values(), abbreviate_dict.keys()))
    for short in rdict.keys():
        if str1.startswith(short):
            str1 = rdict[short] + str1[len(short):]
            break
    if r:
        if attribute and not str1.startswith("attr_"):
            str1 = "attr_" + str1
        elif not attribute and not str1.startswith("rel_"):
            str1 = "rel_" + str1
    return str1


eqv_path = full_fold_path + "/output/" + str(run) + "_eqv.tsv"

eqv = []
eqv_2 = []
with open(eqv_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 3:
            continue
        f = (float)(line[2].strip('%').split(';')[0])
        c = (float)(line[2].strip('%').split(';')[1])
        exp = f * c
        eqv.append((un_abbreviate(line[0]), un_abbreviate(line[1]), exp, (line[2])))
#print(eqv)


sup_path1 = full_fold_path + "/sup_pairs_" + str(excel_num)
with open(sup_path1, 'w', encoding='utf-8') as file:
    for align in eqv:
        if align[2] >= 0.6:
            file.write(f"{align[0]}\t{align[1]}\n")
    
