terraform {
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 4.0.0"
    }
  }
}

# Substitua estes valores pelos que copiou do passo "API Keys" na Oracle
provider "oci" {
 user_ocid="ocid1.user.oc1..aaaaaaaah7i5dmg4b645srjwox2cgnzqxfwtlws7cjnfcaix5k2stei4jvya"
 fingerprint="d1:71:0c:55:50:31:1e:49:76:0a:8d:47:7e:9a:50:02"
 tenancy_ocid="ocid1.tenancy.oc1..aaaaaaaarn367yt7t23yt2ege2kpftxrsqjtvm4el6aznorlmfynb45wieoq"
 region="eu-amsterdam-1"
private_key_path="C:/Users/josed/.ssh/josedieymisson@gmail.com-2025-11-21T18_08_54.143Z.pem"
}