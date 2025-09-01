import argparse
import os
import random
"""
def strip_bracket(str):
    if str[0] == "<":
        str = str[1:]
    if str[-1] == ">":
        str = str[:-1]
    return str   
"""
abbreviate_dict = {"http://dbpedia.org/resource/":"dbp_en:","http://dbpedia.org/property/":"dbp_en_prop:","http://zh.dbpedia.org/resource/":"dbp_zh:","http://zh.dbpedia.org/property/":"dbp_zh_prop:"
                   ,"http://fr.dbpedia.org/resource/":"dbp_fr:","http://fr.dbpedia.org/property/":"dbp_fr_prop:"
                   ,"http://ja.dbpedia.org/resource/":"dbp_ja:","http://ja.dbpedia.org/property/":"dbp_ja_prop:"
                   ,"http://www.w3.org/2001/XMLSchema#":"xsd:","http://dbpedia.org/datatype/":"dbp_type:"
                   ,"http://xmlns.com/foaf/0.1/":"foaf:"
                   ,"http://purl.org/dc/elements/1.1/":"purl:"
                   , "http://dbpedia.org/ontology/":"dbp_onto_prop:"}

def abbreviate(str1, r=0, attribute=0):
    for full in abbreviate_dict.keys():
        if str1.startswith(full):
            str1 = abbreviate_dict[full] + str1[len(full):]
            break
    if r == 1:
        if attribute and not str1.startswith("attr_"):
            str1 = "attr_" + str1
        elif not attribute and not str1.startswith("rel_"):
            str1 = "rel_" + str1
    if r == 2:
        if attribute and str1.startswith("attr_"):
            str1 = str1[len("attr_"):]
        elif not attribute and str1.startswith("rel_"):
            str1 = str1[len("rel_"):]
    return str1

def abbreviate_attribute_value(str1):
    pos = str1.find("^^")
    if pos==-1:
        return abbreviate(str1, 0)
    pos += 2 
    return str1[:pos] + strip_bracket_and_abbreviate(str1[pos:], 0, 0)

def strip_bracket_and_abbreviate(str, r, attribute):
    return abbreviate(str.lstrip('<').rstrip('>'), r, attribute)

def split3(l, with_dot_and_space = 0, attribute = 0, add_r_prefix = True):
    if (not l.rstrip("\n")):
        return None
    if with_dot_and_space:
        list1 = l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2)
    else:
        list1 = l.rstrip("\n").split("\t", maxsplit = 2)
    if (len(list1) != 3):
        if with_dot_and_space:
            list1 = l.rstrip("\n").rstrip(".").rstrip().split(" ", maxsplit = 2)
        else:
            list1 = l.rstrip("\n").split(" ", maxsplit = 2)

        if (len(list1) != 3):
            return None

    (e1, r, a) = list1
    e1 = strip_bracket_and_abbreviate(e1, 0, 0)
    if add_r_prefix == True:
        r = strip_bracket_and_abbreviate(r, 1, attribute)
    else:
        r = strip_bracket_and_abbreviate(r, 2, attribute)
    if attribute:
        a = abbreviate_attribute_value(a)
    else:
        a = strip_bracket_and_abbreviate(a, 0, 0)
    return (e1, r, a)

def main(root_folder, dataset):
    new_dataset_folder = root_folder + "/" + dataset
    #command = "cp -r {} {}".format(root_folder + "/" + dataset, new_dataset_folder)
    #os.system(command)
    ids = {}
    fs1_entities = set()
    #读取19000+有编号（缩减版）实体
    lines = []
    with open(new_dataset_folder + "/ent_ids_1", encoding = "utf8") as f:
        for l in f:
            (id, e1) = l.rstrip("\n").split("\t")
            e1 = abbreviate(e1)
            fs1_entities.add(e1)
            lines.append("{}\t{}\n".format(id, e1))
    with open(new_dataset_folder + "/ent_ids_1", "w", encoding = "utf8") as f:
        f.writelines(lines)

    fs2_entities = set()
    #读取19000+有编号（缩减版）实体
    lines = []
    with open(new_dataset_folder + "/ent_ids_2", encoding = "utf8") as f:
        for l in f:
            (id, e1) = l.rstrip("\n").split("\t")
            e1 = abbreviate(e1)
            fs2_entities.add(e1)
            lines.append("{}\t{}\n".format(id, e1))
    with open(new_dataset_folder + "/ent_ids_2", "w", encoding = "utf8") as f:
        f.writelines(lines)

    lines = []
    with open(new_dataset_folder + "/ent_links", encoding = "utf8") as f:
        e1_not_in = 0
        e2_not_in = 0
        for l in f:
            #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
            if (not l.rstrip("\n")):
                continue
            (e1,e2) = l.rstrip("\n").split("\t", maxsplit = 1)
            e1 = abbreviate(e1)
            e2 = abbreviate(e2)
            if e1 not in fs1_entities:
                print(f"ent_links e1 not in fs1_entities: {e1}")
                e1_not_in += 1
            if e2 not in fs2_entities:
                print(f"ent_links e2 not in fs2_entities: {e2}")
                e2_not_in += 1
            lines.append("{}\t{}\n".format(e1, e2))
        print(f"e1 not in: {e1_not_in}")
        print(f"e2 not in: {e2_not_in}")
    with open(new_dataset_folder + "/ent_links", "w", encoding = "utf8") as f:
        f.writelines(lines)

    lines = []
    attr_1 = set()
    with open(new_dataset_folder + "/attr_triples_1", encoding = "utf8") as f:
        for l in f:
            #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
            tuple1 = split3(l,1,1)
            if (not tuple1):
                continue
            if tuple1[0] not in fs1_entities:
                continue
            lines.append("{}\t{}\t{}\n".format(tuple1[0], tuple1[1], tuple1[2]))
            tuple2 = split3(l,1,1,False)
            attr_1.add(tuple2[1])
    with open(new_dataset_folder + "/attr_triples_1", "w", encoding = "utf8") as f:
        f.writelines(lines)
    lines = []
    attr_2 = set()
    with open(new_dataset_folder + "/attr_triples_2", encoding = "utf8") as f:
        for l in f:
            tuple1 = split3(l,1,1)
            if (not tuple1):
                continue
            if tuple1[0] not in fs2_entities:
                continue
            lines.append("{}\t{}\t{}\n".format(tuple1[0], tuple1[1], tuple1[2]))
            tuple2 = split3(l,1,1,False)
            attr_2.add(tuple2[1])
    with open(new_dataset_folder + "/attr_triples_2", "w", encoding = "utf8") as f:
        f.writelines(lines)


    lines = []
    rel_1 = set()
    rel_triple1 = []
    with open(new_dataset_folder + "/rel_triples_1", encoding = "utf8") as f:
        for l in f:
            #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
            tuple1 = split3(l)
            if (not tuple1):
                continue
            if tuple1[0] not in fs1_entities or tuple1[2] not in fs1_entities :
                continue
            lines.append("{}\t{}\t{}\n".format(tuple1[0], tuple1[1], tuple1[2]))
            tuple2 = split3(l,0,0,False)
            rel_1.add(tuple2[1])
            rel_triple1.append(tuple1)
    with open(new_dataset_folder + "/rel_triples_1", "w", encoding = "utf8") as f:
        f.writelines(lines)

    lines = []
    rel_2 = set()
    rel_triple2 = []
    with open(new_dataset_folder + "/rel_triples_2", encoding = "utf8") as f:
        for l in f:
            tuple1 = split3(l)
            if (not tuple1):
                continue
            if tuple1[0] not in fs2_entities or tuple1[2] not in fs2_entities :
                continue
            lines.append("{}\t{}\t{}\n".format(tuple1[0], tuple1[1], tuple1[2]))
            tuple2 = split3(l,0,0,False)
            rel_2.add(tuple2[1])
            rel_triple2.append(tuple2)
    with open(new_dataset_folder + "/rel_triples_2", "w", encoding = "utf8") as f:
        f.writelines(lines)
    
    print(f"len(attr_1):{len(attr_1)}")
    list_attr_1 = list(attr_1)
    print(list_attr_1[0],list_attr_1[1],list_attr_1[2])
    print(f"len(rel_1):{len(rel_1)}")
    list_rel_1 = list(rel_1)
    print(list_rel_1[0],list_rel_1[1],list_rel_1[2])
    print(f"len(attr_1 & rel_1):{len(attr_1 & rel_1)}")
    print(f"len(attr_2):{len(attr_2)}")
    print(f"len(rel_2):{len(rel_2)}")
    print(f"len(attr_2 & rel_2):{len(attr_1 & rel_1)}")
    print()

    rel_triples = dict()
    for t in rel_triple1:
        if t[0] not in rel_triples:
            rel_triples[t[0]] = [t]
        else:
            rel_triples[t[0]].append(t)
    lines = []
    for e in rel_triples.keys():
        rel_list = []
        rel_list_done = []
        for t in rel_triples[e]:
            if t[1] not in rel_list_done:
                if t[1] in rel_list:
                    for t2 in rel_triples[e]:
                        if t2[1]==t[1]:
                            lines.append("{}\t{}\t{}\n".format(t2[0], t2[1], t2[2]))
                    rel_list_done.append(t[1])
                else:
                    rel_list.append(t[1])
    with open(new_dataset_folder + "/rel_triple1_same_h_r", "w", encoding = "utf8") as f:
        f.writelines(lines)
    rel_triples_r = dict()
    for t in rel_triple1:
        if t[1] not in rel_triples_r:
            rel_triples_r[t[1]] = [t]
        else:
            rel_triples_r[t[1]].append(t)
    lines = []
    for r in rel_triples_r.keys():
        for e in rel_triples_r[r]:
            lines.append("{}\t{}\t{}\n".format(e[0], e[1], e[2]))
    with open(new_dataset_folder + "/rel_triple1_same_r", "w", encoding = "utf8") as f:
        f.writelines(lines)






if __name__ == "__main__":
    """
    parser = argparse.ArgumentParser(
        description="Anonymize the given dataset")

    parser.add_argument(
        "--root_folder",
        type=str,
        help='Name of the root directory (it will be used even as output root.)'
    )

    parser.add_argument(
        "--dataset",
        help="Name of the file containing the dataset",
        required=True
    )

    args = parser.parse_args()
    """
    main("/home/2022xuch/PNAL/datasets", "DBP15k_full_zh_en_2")
    #main("/home/2022xuch/PNAL/datasets", "DBP15k_full_ja_en_2")
    #main("/home/2022xuch/PNAL/datasets", "DBP15k_full_fr_en_2")
