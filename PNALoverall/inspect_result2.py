import os
from utils import * 
import functools
import random
output_path = "/home/2022xuch/PNAL/PNALoverall/output"
#lang_path = "/zh_en"
fold_path = "/DBP15k_full_zh_en_2_0604_103642"
full_fold_path = output_path + fold_path

zero_seed = True
filter = 0#0.1

dataset_path = "/home/2022xuch/PNAL/datasets/DBP15k_full_zh_en_2"
run = 21 #21
eqv_path = full_fold_path + "/output/" + str(run) + "_eqv.tsv"
train_path = full_fold_path + "/" + "train" + "_links"
valid_path = full_fold_path + "/" + "valid" + "_links"
test_path = full_fold_path + "/" + "test" + "_links"
inspect_path = full_fold_path + "/inspect"

attr_path = dataset_path + "/attr_triples"
attr_paths = [attr_path + "_1", attr_path + "_2"]
rel_path = dataset_path + "/rel_triples"
rel_paths = [rel_path + "_1", rel_path + "_2"]

eqv = []
eqv_2 = []
eqv_dict = dict()
with open(eqv_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 3:
            continue
        f = (float)(line[2].strip('%').split(';')[0])
        c = (float)(line[2].strip('%').split(';')[1])
        exp = f * c
        if exp > filter:
            eqv.append((line[0], line[1], exp, (line[2])))
            eqv_2.append((line[0], line[1]))
            eqv_dict[line[0]] = line[1]

if not os.path.exists(inspect_path):
    os.mkdir(inspect_path)


inspect_path1 = inspect_path + "/inspect_" + str(run) + "_eqv.tsv"




def find_path3(relation_name1, relation_name2):
    rel_list1 = []
    with open(rel_paths[0], 'r', encoding='utf-8') as file:
        for line in file:
            line_split = line.strip('\n').split('\t')
            if len(line_split)!= 3:
                continue
            if line_split[1] == relation_name1:
                rel_list1.append((line_split[0],line_split[1],line_split[2]))
        rel_list1.sort(key = lambda x:x[0])
    #print(rel_list1)
    rel_list2 = []
    with open(rel_paths[1], 'r', encoding='utf-8') as file:
        for line in file:
            line_split = line.strip('\n').split('\t')
            if len(line_split)!= 3:
                continue
            if line_split[1] == relation_name2:
                rel_list2.append((line_split[0],line_split[1],line_split[2]))
        rel_list2.sort(key = lambda x:x[0])
    #print(rel_list2)
    for triple in rel_list1:
        head = eqv_dict.get(triple[0])
        tail = eqv_dict.get(triple[2])
        if head is None or tail is None:
            continue
        #print(head,tail)
        for triple2 in rel_list2:
            if head == triple2[0] and tail == triple2[2]:
                print(f"{triple[0]}    {triple[1]}    {triple[2]}\n")
                print(f"{triple2[0]}    {triple2[1]}    {triple2[2]}\n")

find_path3("rel_dbp_zh_prop:writer", "rel_dbp_en_prop:artist")
