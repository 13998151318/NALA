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
abbreviate_dict = { "http://www.wikidata.org/entity/":"wiki:", "http://dbpedia.org/ontology/":"dbp_prop:", "http://schema.org/":"schema:",
    "http://dbpedia.org/resource/":"dbp_en:","http://dbpedia.org/property/":"dbp_en_prop:","http://zh.dbpedia.org/resource/":"dbp_zh:","http://zh.dbpedia.org/property/":"dbp_zh_prop:"
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

def abbreviate_attribute_value(str1):
    pos = str1.find("^^")
    if pos==-1:
        return abbreviate(str1, 0)
    pos += 2 
    return str1[:pos] + strip_bracket_and_abbreviate(str1[pos:], 0, 0)

def strip_bracket_and_abbreviate(str, r, attribute):
    return abbreviate(str.lstrip('<').rstrip('>'), r, attribute)

def split3(l, with_dot_and_space = 0, attribute = 0):
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
    r = strip_bracket_and_abbreviate(r, 1, attribute)
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
    lines = []
    with open(new_dataset_folder + "/ent_links", encoding = "utf8") as f:
        for l in f:
            #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
            if (not l.rstrip("\n")):
                continue
            (e1,e2) = l.rstrip("\n").split("\t", maxsplit = 1)
            e1 = abbreviate(e1)
            e2 = abbreviate(e2)
            lines.append("{}\t{}\n".format(e1, e2))
    with open(new_dataset_folder + "/ent_links", "w", encoding = "utf8") as f:
        f.writelines(lines)

    lines = []
    with open(new_dataset_folder + "/attr_triples_1", encoding = "utf8") as f:
        for l in f:
            #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
            tuple1 = split3(l,1,1)
            if (not tuple1):
                continue
            lines.append("{}\t{}\t{}\n".format(tuple1[0], tuple1[1], tuple1[2]))
    with open(new_dataset_folder + "/attr_triples_1", "w", encoding = "utf8") as f:
        f.writelines(lines)
    lines = []
    with open(new_dataset_folder + "/attr_triples_2", encoding = "utf8") as f:
        for l in f:
            tuple1 = split3(l,1,1)
            if (not tuple1):
                continue
            lines.append("{}\t{}\t{}\n".format(tuple1[0], tuple1[1], tuple1[2]))
    with open(new_dataset_folder + "/attr_triples_2", "w", encoding = "utf8") as f:
        f.writelines(lines)


    lines = []
    with open(new_dataset_folder + "/rel_triples_1", encoding = "utf8") as f:
        for l in f:
            #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
            tuple1 = split3(l)
            if (not tuple1):
                continue
            lines.append("{}\t{}\t{}\n".format(tuple1[0], tuple1[1], tuple1[2]))
    with open(new_dataset_folder + "/rel_triples_1", "w", encoding = "utf8") as f:
        f.writelines(lines)

    lines = []
    with open(new_dataset_folder + "/rel_triples_2", encoding = "utf8") as f:
        for l in f:
            tuple1 = split3(l)
            if (not tuple1):
                continue
            lines.append("{}\t{}\t{}\n".format(tuple1[0], tuple1[1], tuple1[2]))
    with open(new_dataset_folder + "/rel_triples_2", "w", encoding = "utf8") as f:
        f.writelines(lines)
    
    for fold in os.listdir(new_dataset_folder + "/721_5fold"):
        for name in ["test_links", "train_links", "valid_links"]:
            foldi = new_dataset_folder + "/721_5fold/" + fold + "/" + name
            lines = []
            with open(foldi, encoding = "utf8") as f:
                for l in f:
                    #print(l.rstrip("\n").rstrip(".").rstrip().split("\t", maxsplit = 2))
                    if (not l.rstrip("\n")):
                        continue
                    (e1,e2) = l.rstrip("\n").split("\t", maxsplit = 1)
                    e1 = abbreviate(e1)
                    e2 = abbreviate(e2)
                    lines.append("{}\t{}\n".format(e1, e2))
            with open(foldi, "w", encoding = "utf8") as f:
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
    main("/home/2022xuch/PNAL/datasets", "D_W_15K_V2")
    main("/home/2022xuch/PNAL/datasets", "D_W_100K_V2")
    main("/home/2022xuch/PNAL/datasets", "D_Y_15K_V2")
    main("/home/2022xuch/PNAL/datasets", "D_Y_100K_V2")

    main("/home/2022xuch/PNAL/datasets", "D_W_15K_V1")
    main("/home/2022xuch/PNAL/datasets", "D_W_100K_V1")
    main("/home/2022xuch/PNAL/datasets", "D_Y_15K_V1")
    main("/home/2022xuch/PNAL/datasets", "D_Y_100K_V1")

