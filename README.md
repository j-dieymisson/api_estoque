# 📦 CEPRA Estoque — Sistema de Gestão de Ativos

[![Java](https://img.shields.io/badge/Java-17-informational?logo=java\&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=spring\&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql\&logoColor=white)](https://www.mysql.com/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5-7952B3?logo=bootstrap\&logoColor=white)](https://getbootstrap.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker\&logoColor=white)](https://www.docker.com/)
[![Nginx](https://img.shields.io/badge/Nginx-Reverse%20Proxy-009639?logo=nginx\&logoColor=white)](https://nginx.org/)
[![Terraform](https://img.shields.io/badge/Terraform-OCI-7B6CFF?logo=terraform\&logoColor=white)](https://www.terraform.io/)
[![HTML5](https://img.shields.io/badge/HTML5-%3E%3D5-E34F26?logo=html5\&logoColor=white)](https://developer.mozilla.org/docs/Web/Guide/HTML/HTML5)
[![CSS3](https://img.shields.io/badge/CSS3-%3E%3D3-1572B6?logo=css3\&logoColor=white)](https://developer.mozilla.org/docs/Web/CSS)
[![JavaScript](https://img.shields.io/badge/JavaScript-%3E%3DES6-F7DF1E?logo=javascript\&logoColor=black)](https://developer.mozilla.org/docs/Web/JavaScript)

---

## ⚙️ Visão Geral

O **CEPRA Estoque** é uma plataforma web enterprise para digitalização do inventário e automatização do fluxo de aprovações. Substitui processos manuais (papel) por um fluxo digital auditável, seguro e com controlo em tempo real de stock e aprovações em múltiplos níveis.

## 🎯 Principais Recursos

* Autenticação stateless via **JWT**.
* Controle de acesso por **RBAC**: `ADMIN`, `GESTOR`, `COLABORADOR`.
* Cadastro completo de ativos, categorização e controlo de quantidade total vs disponível.
* Histórico imutável (audit trail) de todas as movimentações.
* Workflow de aprovação inteligente com roteamento automático entre níveis de decisão.
* Envio de notificações por e-mail (SMTP — Gmail) em cada alteração de estado.
* Dashboard com métricas em tempo real e geração dinâmica de PDFs (comprovante de entrega).

## 🎨 Design & UX

* Construído com **Bootstrap 5** para responsividade e componentes modernos.
* **Dark Mode** nativo (`data-bs-theme="dark"`) com semântica de cores (verde = sucesso, amarelo = pendente).
* Navegação: sidebar expansível no desktop; offcanvas no mobile.
* Feedback: toasts para ações e modais para confirmações críticas.

## 🏗️ Arquitetura & Infraestrutura

* **Aplicação:** Spring Boot 3 (Java 17) — porta interna `8080`.
* **Banco:** MySQL 8.0 com persistência via volumes Docker.
* **Gateway / Proxy:** Nginx (porta 80/443) com terminação SSL.
* **Infraestrutura:** Provisionada na Oracle Cloud (OCI) via Terraform.
* Pronto para ser executado em containers com Docker Compose.

## 🔐 Variáveis de Ambiente (exemplo `.env`)

```env
DB_HOST=localhost
DB_PORT=3306
DB_USER_ESTOQUE=root
DB_PASS_ESTOQUE=SuaSenhaForte123
DB_NAME_ESTOQUE=cepra_estoque

JWT_SECRET_ESTOQUE=ChaveSecretaSuperLongaHash...

MAIL_USERNAME=seu.email@gmail.com
MAIL_PASSWORD=SenhaAppGmail

SPRING_PROFILES_ACTIVE=prod
```

> **Importante:** Nunca commite arquivos `.env` com segredos em repositórios públicos. Use variáveis de ambiente do provedor/CI ou soluções de secret manager.

## 🐳 Rodando Localmente (Docker)

**Pré-requisitos**: Docker Desktop e Git.

```bash
# clonar repo
git clone https://github.com/seu-usuario/api-estoque.git
cd api-estoque

# criar .env com as variáveis acima

# compilar (gera target/app.jar)
./mvnw clean package -DskipTests

# subir containers
docker compose up -d --build
```

A aplicação ficará disponível em `http://localhost/cepra` (ou conforme configuração do Nginx/reverse-proxy local).

## ☁️ Deploy (Oracle Cloud) — resumo do fluxo

1. Na pasta `infra`, configure `terraform.tfvars` com seus OCIDs/credenciais.
2. Execute `terraform init` e `terraform apply` para provisionar recursos.
3. Transfira artefatos para a VM:

```bash
scp -i chave.key docker-compose.yml opc@IP:/opt/api-estoque/
scp -i chave.key nginx.conf opc@IP:/opt/api-estoque/
scp -i chave.key target/*.jar opc@IP:/opt/api-estoque/app.jar
```

4. Conecte-se via SSH e inicie:

```bash
ssh -i chave.key opc@IP
cd /opt/api-estoque
# crie .env de produção
docker compose up -d --build
```

## 📚 Documentação da API

A documentação Swagger está disponível quando a aplicação está em execução:

```
http://localhost/cepra/swagger-ui.html
```

## 🧩 Fluxo de Aprovação (resumido)

1. Usuário cria pedido de equipamento.
2. Motor de estados verifica hierarquia e roteia para `GESTOR` (Nível 1) ou `ADMIN` (Nível 2).
3. Cada alteração registra: quem aprovou, quando, e motivo em caso de recusa.
4. Notificações transacionais são disparadas por e-mail.


## 👨‍💻 Autor

**José Dieymisson Barros** — Projeto de conclusão de curso e estágio na CEPRA.

---
