variable "compartment_ocid" {
  # Cole aqui o OCID do seu compartimento (Pode ser o mesmo do Tenancy se não criou outro)
  default = "ocid1.user.oc1..aaaaaaaah7i5dmg4b645srjwox2cgnzqxfwtlws7cjnfcaix5k2stei4jvya"
}

variable "ssh_public_key" {
  # Cole aqui o conteúdo do ficheiro oci_key.pub que gerou no passo 1
  default = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQC9yzEkzyBqssghlGZx2NzmiloJIZt45CkhZSDuMkkCEzPhwbDwUlSZP2Itit9NmHMOsSG9D5Y6Iwmb7LU3LTdlk/NPp5L5cRSj0NcUpf5ebZ18zRiqQAGXfGW4p6lEnOpusT1jf+PgXCR6kH1f0CvqPtcJKt4hs4OkO/nYECezbmMrY+RPXjPmLUrpUH6vX17ZQjKbfVG27d41UZ6vJ0Xb04+UFJbard4lg9GuHxACrNMsiz+WCUG2pg3se1phdrV7qyl9Kx2pQmoUe4sfN6aj7jTl5KLneSewQ9KMiM3olXpIRX2giqu7MJo/sPJssSIbQqwyU7DHRVu7EPPyVObCEJLxzTe6fy1A6GYdOhtec7OewJI43GIANNnpaKz19eBaJxKOu2oVCANlIatuL05npyY27eFfcf3dZJmCFMtoZ/a3swDatDD8mt/tJd0S8D6gqy/qfJ8P+JBYq6+pvCsW5p5BOG+kHOfpKM4f0mLd3BfRPNNJT77jydOzjRYc5fw0z+lKqjWi1GzooOE8ovKmj3opgTRpysXLcClL3AVX89Wn4GIZVqq8cviFsvUgm665vT7fdWwEKLrNyje5to9zBJ8OYS8h28X3ADbInrkllPIOEkGDxeMNEsw6VcdHLatNA844xxuHXvQel8xmn8hdbtVHMbYSfA7+YvYGivp4Rw== josed@Dieymisson"
}

# Busca os Domínios de Disponibilidade (ADs)
data "oci_identity_availability_domains" "ads" {
  compartment_id = var.compartment_ocid
}

# Busca Imagem para a Máquina AMD (Micro)
data "oci_core_images" "oracle_linux_8_amd" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Oracle Linux"
  operating_system_version = "8"
  shape                    = "VM.Standard.E2.1.Micro"
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

# Busca Imagem para a Máquina ARM (Potente)
data "oci_core_images" "oracle_linux_8_arm" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Oracle Linux"
  operating_system_version = "8"
  shape                    = "VM.Standard.A1.Flex"
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

# ==============================================================================
# 2. REDE (VCN e Segurança)
# ==============================================================================

resource "oci_core_vcn" "estoque_vcn" {
  cidr_block     = "10.0.0.0/16"
  compartment_id = var.compartment_ocid
  display_name   = "EstoqueVCN"
}

resource "oci_core_internet_gateway" "estoque_ig" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.estoque_vcn.id
  display_name   = "EstoqueIG"
}

resource "oci_core_route_table" "estoque_rt" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.estoque_vcn.id
  display_name   = "EstoqueRouteTable"

  route_rules {
    destination       = "0.0.0.0/0"
    network_entity_id = oci_core_internet_gateway.estoque_ig.id
  }
}

resource "oci_core_security_list" "estoque_sl" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.estoque_vcn.id
  display_name   = "EstoqueSecurityList"

  ingress_security_rules { # SSH
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 22
      max = 22
    }
  }
  ingress_security_rules { # API
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 8080
      max = 8080
    }
  }
  egress_security_rules {
    protocol    = "all"
    destination = "0.0.0.0/0"
  }
}

resource "oci_core_subnet" "estoque_subnet" {
  cidr_block        = "10.0.1.0/24"
  compartment_id    = var.compartment_ocid
  vcn_id            = oci_core_vcn.estoque_vcn.id
  display_name      = "EstoqueSubnetPublica"
  route_table_id    = oci_core_route_table.estoque_rt.id
  security_list_ids = [oci_core_security_list.estoque_sl.id]
}

# ==============================================================================
# 3. MÁQUINA 1: AMD MICRO (Backup/Atual)
# ==============================================================================

resource "oci_core_instance" "estoque_server" {
  # Tenta usar o primeiro AD. Se der erro de AD, pode fixar manualmente (ex: "BmXK:...")
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  compartment_id      = var.compartment_ocid
  display_name        = "ServidorEstoque-AMD"
  shape               = "VM.Standard.E2.1.Micro"

  create_vnic_details {
    subnet_id        = oci_core_subnet.estoque_subnet.id
    assign_public_ip = true
  }

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.oracle_linux_8_amd.images[0].id
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key

    # Script COM SWAP (Para 1GB RAM)
    user_data = base64encode(<<-EOF
      #!/bin/bash
      fallocate -l 2G /swapfile
      chmod 600 /swapfile
      mkswap /swapfile
      swapon /swapfile
      echo '/swapfile none swap sw 0 0' >> /etc/fstab

      dnf update -y
      dnf install -y dnf-utils zip unzip
      dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo
      dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
      systemctl enable docker
      systemctl start docker
      usermod -aG docker opc
      firewall-cmd --permanent --add-port=8080/tcp
      firewall-cmd --reload
    EOF
    )
  }
}

# ==============================================================================
# 4. MÁQUINA 2: ARM AMPERE (A Potente - Alvo do Script Sniper)
# ==============================================================================

resource "oci_core_instance" "estoque_server_arm" {
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
  compartment_id      = var.compartment_ocid
  display_name        = "ServidorEstoque-ARM-Potente"
  shape               = "VM.Standard.A1.Flex"

  shape_config {
    ocpus         = 4
    memory_in_gbs = 24
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.estoque_subnet.id
    assign_public_ip = true
  }

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.oracle_linux_8_arm.images[0].id
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key

    # Script SEM SWAP (Não precisa para 24GB RAM)
    user_data = base64encode(<<-EOF
      #!/bin/bash
      dnf update -y
      dnf install -y dnf-utils zip unzip
      dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo
      dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
      systemctl enable docker
      systemctl start docker
      usermod -aG docker opc
      firewall-cmd --permanent --add-port=8080/tcp
      firewall-cmd --reload
    EOF
    )
  }
}

# ==============================================================================
# 5. OUTPUTS
# ==============================================================================

output "ip_publico_amd" {
  value = oci_core_instance.estoque_server.public_ip
  description = "O IP público da máquina Micro (AMD)"
}

output "ip_publico_arm" {
  value = oci_core_instance.estoque_server_arm.public_ip
  description = "O IP público da máquina Potente (ARM)"
}