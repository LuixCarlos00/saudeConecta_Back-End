#!/usr/bin/env python3
"""Gera docs/demo/seed-demo.sql: base de demonstração do SaúdeConecta (MySQL 8).

O .sql gerado APAGA os dados de negócio e repopula tudo com datas relativas à data
de importação (CURDATE/NOW em horário de Brasília). Ver docs/demo/README.md.

Uso:
    python3 docs/demo/gerar_seed_demo.py                 # seed fixa (20261105)
    python3 docs/demo/gerar_seed_demo.py --seed 42       # outra massa aleatória
    python3 docs/demo/gerar_seed_demo.py --saida /tmp/x.sql

Dependências: stdlib + bcrypt (hash no formato $2a$10$ do BCryptPasswordEncoder do Spring).
"""

import argparse
import json
import random
import uuid
from pathlib import Path

import bcrypt

SEED_PADRAO = 20261105
LOGIN_ROOT = "14776815664"
EMAIL_ROOT = "luixcarlos00@gmail.com"  # mesmo e-mail gravado pelo changeset 046

# Dias úteis gerados para trás (-DIAS_UTEIS..-1) e para frente (0..DIAS_UTEIS).
# 24 dias úteis ~= 33-34 dias corridos: garante >= 30 dias corridos de cada lado.
DIAS_UTEIS = 24

ASSINATURA_PNG = ("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=")

# --------------------------------------------------------------------------------------
# Tenants. limites_custom = (admin, profissional, secretaria); None = usa o limite do plano.
# --------------------------------------------------------------------------------------
TENANTS = [
    dict(chave="A", nome="Clínica Vida Plena", razao="Vida Plena Serviços Médicos e Odontológicos Ltda",
         tipo="MISTA", plano="BUSINESS", gestores=2, secretarias=4,
         medicos=["CARDIO", "DERMA", "MED_CLINICA_MEDICA", "GINE"],
         dentistas=["ORTODONT", "ENDO", "IMPL", "GERAL"],
         cidade="São Paulo", inicio_dias=160, pular_confirmado=False, custom=None, cortesia=False),
    dict(chave="B", nome="Centro Médico Bem Viver", razao="Bem Viver Centro Médico Ltda",
         tipo="MISTA", plano="PROFISSIONAL", gestores=2, secretarias=2,
         medicos=["PED", "ORTO", "MED_ENDOCRINOLOGIA"], dentistas=["PERIO"],
         cidade="Campinas", inicio_dias=120, pular_confirmado=True, custom=None, cortesia=False),
    dict(chave="C", nome="Consultório Saúde & Sorriso", razao="Saúde e Sorriso Consultório Ltda ME",
         tipo="CONSULTORIO", plano="STARTER", gestores=1, secretarias=1,
         medicos=["MED_CLINICA_MEDICA"], dentistas=["GERAL"],
         cidade="Curitiba", inicio_dias=75, pular_confirmado=False, custom=None, cortesia=False),
    # Plano personalizado: STARTER + perfis contratados à parte (extras pagos na mensalidade).
    dict(chave="D", nome="Clínica Integrada Horizonte", razao="Horizonte Clínica Integrada Ltda",
         tipo="MISTA", plano="STARTER", gestores=2, secretarias=3,
         medicos=["MED_OTORRINOLARINGOLOGIA", "DERMA"], dentistas=["ORTODONT", "PROT", "ENDO"],
         cidade="Belo Horizonte", inicio_dias=100, pular_confirmado=False, custom=(2, 5, 3), cortesia=False),
    # Plano personalizado + cortesia: PROFISSIONAL com gestor e clínicos extras, sem cobrança.
    dict(chave="E", nome="Policlínica Mar Azul", razao="Mar Azul Policlínica Ltda",
         tipo="CLINICA", plano="PROFISSIONAL", gestores=3, secretarias=3,
         medicos=["CARDIO", "PED", "ORTO", "MED_CLINICA_MEDICA"], dentistas=["IMPL", "GERAL"],
         cidade="Recife", inicio_dias=90, pular_confirmado=False, custom=(3, 10, None), cortesia=True),
]

# Valores e limites dos planos (changesets 027/054). Usados só para calcular extras da personalização.
PLANOS = {
    "STARTER": dict(valor=149.00, limites=(1, 2, 1)),
    "PROFISSIONAL": dict(valor=279.00, limites=(2, 8, 4)),
    "BUSINESS": dict(valor=549.00, limites=(5, 20, 20)),
}
VALOR_EXTRA = (59.00, 39.00, 29.00)  # admin, profissional, secretaria (valor_adicional_* do plano)

CIDADES = {
    "São Paulo": dict(uf="SP", ddd="11", cep=(1000, 5999), bairros=["Moema", "Vila Mariana", "Pinheiros", "Tatuapé", "Santana", "Perdizes", "Itaim Bibi", "Saúde"]),
    "Campinas": dict(uf="SP", ddd="19", cep=(13010, 13139), bairros=["Cambuí", "Taquaral", "Barão Geraldo", "Guanabara", "Castelo", "Jardim Chapadão"]),
    "Curitiba": dict(uf="PR", ddd="41", cep=(80010, 82999), bairros=["Batel", "Água Verde", "Bigorrilho", "Centro Cívico", "Portão", "Cabral"]),
    "Belo Horizonte": dict(uf="MG", ddd="31", cep=(30110, 31999), bairros=["Savassi", "Funcionários", "Lourdes", "Pampulha", "Buritis", "Santa Efigênia"]),
    "Recife": dict(uf="PE", ddd="81", cep=(50010, 52999), bairros=["Boa Viagem", "Espinheiro", "Graças", "Casa Forte", "Madalena", "Pina"]),
}
RUAS = ["Rua das Flores", "Avenida Brasil", "Rua Sete de Setembro", "Rua XV de Novembro", "Avenida Paulista",
        "Rua Tiradentes", "Rua Dom Pedro II", "Avenida Getúlio Vargas", "Rua São João", "Rua Marechal Deodoro",
        "Rua Santos Dumont", "Avenida Rio Branco", "Rua Padre Anchieta", "Rua José Bonifácio", "Rua Rui Barbosa",
        "Alameda Santos", "Rua Bela Vista", "Rua das Palmeiras", "Rua Carlos Gomes", "Avenida Independência"]

NOMES_M = ["Lucas", "Gabriel", "Rafael", "Pedro", "Matheus", "Gustavo", "Felipe", "Bruno", "Thiago", "Rodrigo",
           "André", "Eduardo", "Marcelo", "Diego", "Leonardo", "Vinícius", "Daniel", "Henrique", "Caio", "Fábio",
           "João", "José", "Antônio", "Carlos", "Paulo", "Ricardo", "Fernando", "Luiz", "Sérgio", "Otávio",
           "Miguel", "Arthur", "Heitor", "Bernardo", "Davi", "Samuel", "Enzo", "Lorenzo", "Murilo", "Igor"]
NOMES_F = ["Ana", "Mariana", "Juliana", "Camila", "Fernanda", "Beatriz", "Larissa", "Amanda", "Letícia", "Patrícia",
           "Gabriela", "Carolina", "Aline", "Renata", "Vanessa", "Bruna", "Natália", "Isabela", "Priscila", "Débora",
           "Maria", "Luana", "Tatiane", "Cláudia", "Helena", "Alice", "Laura", "Manuela", "Valentina", "Sofia",
           "Lívia", "Cecília", "Sandra", "Márcia", "Rosana", "Elaine", "Simone", "Viviane", "Raquel", "Bianca"]
SOBRENOMES = ["Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Alves", "Pereira", "Lima", "Gomes",
              "Costa", "Ribeiro", "Martins", "Carvalho", "Almeida", "Lopes", "Soares", "Fernandes", "Vieira", "Barbosa",
              "Rocha", "Dias", "Nascimento", "Andrade", "Moreira", "Nunes", "Marques", "Machado", "Mendes", "Freitas",
              "Cardoso", "Ramos", "Gonçalves", "Santana", "Teixeira", "Araújo", "Pinto", "Correia", "Batista", "Moura"]
INSTITUICOES_MED = ["Universidade de São Paulo (USP)", "Universidade Federal de Minas Gerais (UFMG)", "Universidade Estadual de Campinas (Unicamp)",
                    "Universidade Federal do Paraná (UFPR)", "Universidade Federal de Pernambuco (UFPE)", "Universidade Federal de São Paulo (Unifesp)"]

FORMAS_PAGAMENTO = [("PIX", 35), ("CREDITO", 25), ("DEBITO", 15), ("DINHEIRO", 10), ("CONVENIO", 10), ("PARTICULAR", 5)]

PERGUNTAS_QUESTIONARIO = [
    "Está sob tratamento médico?", "Está tomando algum medicamento? Qual?", "É alérgico a algum medicamento ou substância?",
    "Já foi submetido a alguma cirurgia?", "Já teve alguma reação a anestesia?", "Tem ou teve algum problema cardíaco?",
    "Tem pressão alta ou baixa?", "Tem diabetes?", "Tem problemas de coagulação ou sangramento?",
    "Tem hepatite, HIV ou outra doença infectocontagiosa?", "Tem problema respiratório (asma, bronquite)?",
    "Tem epilepsia ou convulsões?", "Tem problemas renais?", "Tem problemas gástricos (úlcera, gastrite)?",
    "Está grávida ou amamentando?", "É fumante?", "Consome bebidas alcoólicas com frequência?",
    "Tem alguma outra doença ou condição não mencionada?",
]
OBS_SIM = {1: "Losartana 50 mg uma vez ao dia", 2: "Alergia a dipirona", 3: "Apendicectomia em 2015",
           6: "Hipertensão controlada", 7: "Diabetes tipo 2 controlado com metformina", 13: "Gastrite leve", 15: "Cerca de 10 cigarros por dia"}

# --------------------------------------------------------------------------------------
# Casos clínicos (texto livre, como o front grava). "procs" vira planejamento terapêutico.
# --------------------------------------------------------------------------------------
CASOS_MEDICO = {
    "CARDIO": [
        dict(queixa="Palpitações e cansaço aos esforços há 2 meses.", anamnese="Paciente relata palpitações esporádicas, piora ao subir escadas. Nega síncope. Histórico familiar de hipertensão.",
             diagnostico="Hipertensão arterial sistêmica estágio 1, sem lesão de órgão-alvo.", cid="I10 - Hipertensão essencial (primária)",
             tit="Anti-hipertensivo", presc="Losartana potássica 50 mg - 1 comprimido via oral pela manhã, uso contínuo.",
             orient="Reduzir sal na dieta, caminhada 30 minutos 5x por semana, monitorar pressão em casa.",
             exames="Hemograma completo; Creatinina; Potássio; Perfil lipídico; Ecocardiograma transtorácico.", tuss="10101012 - Consulta em consultório",
             procs=[("Eletrocardiograma de repouso", 120.00)]),
        dict(queixa="Dor torácica atípica após atividade física.", anamnese="Dor em pontada, sem irradiação, melhora com repouso. Sedentário, IMC elevado.",
             diagnostico="Dor torácica não cardíaca a esclarecer; dislipidemia.", cid="E78.5 - Hiperlipidemia não especificada",
             tit="Hipolipemiante", presc="Atorvastatina 20 mg - 1 comprimido à noite por 90 dias.",
             orient="Dieta pobre em gorduras saturadas; retornar com exames.",
             exames="Teste ergométrico; Perfil lipídico; Glicemia de jejum.", tuss="10101012 - Consulta em consultório",
             procs=[("Teste ergométrico", 280.00)]),
    ],
    "DERMA": [
        dict(queixa="Manchas avermelhadas e descamativas nos cotovelos.", anamnese="Lesões há 6 meses, pruriginosas, pioram no inverno. Pai com quadro semelhante.",
             diagnostico="Psoríase em placas leve.", cid="L40.0 - Psoríase vulgar",
             tit="Tratamento tópico", presc="Clobetasol 0,05% creme - aplicar 2x ao dia nas lesões por 14 dias.",
             orient="Hidratar a pele diariamente, evitar banhos muito quentes.", exames="Não se aplica.", tuss="10101012 - Consulta em consultório", procs=[]),
        dict(queixa="Pinta escura nas costas que mudou de cor.", anamnese="Lesão pigmentada com bordas irregulares notada pela esposa. Exposição solar frequente.",
             diagnostico="Nevo melanocítico atípico - indicada exérese.", cid="D22.5 - Nevo melanocítico do tronco",
             tit="Fotoproteção", presc="Protetor solar FPS 50 - reaplicar a cada 3 horas.",
             orient="Evitar exposição solar entre 10h e 16h.", exames="Dermatoscopia digital; Anatomopatológico após exérese.", tuss="10101012 - Consulta em consultório",
             procs=[("Exérese de lesão de pele com anatomopatológico", 450.00)]),
    ],
    "MED_CLINICA_MEDICA": [
        dict(queixa="Cansaço, sede excessiva e aumento da frequência urinária.", anamnese="Sintomas há 3 meses. Mãe diabética. Ganho de peso no último ano.",
             diagnostico="Diabetes mellitus tipo 2 recém-diagnosticado.", cid="E11.9 - Diabetes mellitus tipo 2 sem complicações",
             tit="Hipoglicemiante oral", presc="Metformina 850 mg - 1 comprimido após almoço e jantar.",
             orient="Dieta com baixo índice glicêmico, atividade física regular, monitorar glicemia.",
             exames="Hemoglobina glicada; Glicemia de jejum; Microalbuminúria; Perfil lipídico.", tuss="10101012 - Consulta em consultório", procs=[]),
        dict(queixa="Dor de garganta, febre e coriza há 3 dias.", anamnese="Febre de até 38,5 °C, odinofagia. Nega dispneia.",
             diagnostico="Faringoamigdalite viral.", cid="J02.9 - Faringite aguda não especificada",
             tit="Sintomáticos", presc="Paracetamol 750 mg - 1 comprimido a cada 6 horas se febre ou dor, por até 5 dias.",
             orient="Hidratação, repouso, retornar se febre persistir por mais de 72 horas.", exames="Não se aplica.", tuss="10101012 - Consulta em consultório", procs=[]),
    ],
    "GINE": [
        dict(queixa="Consulta de rotina e preventivo.", anamnese="Ciclos regulares, uso de anticoncepcional oral. Último preventivo há 2 anos.",
             diagnostico="Exame ginecológico sem alterações.", cid="Z01.4 - Exame ginecológico (geral) (de rotina)",
             tit="Suplementação", presc="Ácido fólico 5 mg - 1 comprimido ao dia.", orient="Manter acompanhamento anual.",
             exames="Colpocitologia oncótica; Ultrassonografia transvaginal; Mamografia.", tuss="10101012 - Consulta em consultório",
             procs=[("Coleta de colpocitologia oncótica", 90.00)]),
        dict(queixa="Cólicas menstruais intensas.", anamnese="Dismenorreia progressiva há 1 ano, com dor pélvica fora do período menstrual.",
             diagnostico="Suspeita de endometriose.", cid="N80.9 - Endometriose não especificada",
             tit="Analgesia", presc="Ibuprofeno 600 mg - 1 comprimido a cada 8 horas durante a menstruação.",
             orient="Diário de sintomas até o retorno.", exames="Ultrassonografia pélvica com preparo intestinal; CA-125.", tuss="10101012 - Consulta em consultório", procs=[]),
    ],
    "PED": [
        dict(queixa="Tosse e chiado no peito há 5 dias.", anamnese="Criança com episódios prévios de sibilância. Sem febre. Boa aceitação alimentar.",
             diagnostico="Asma leve intermitente.", cid="J45.9 - Asma não especificada",
             tit="Broncodilatador", presc="Salbutamol spray 100 mcg com espaçador - 2 jatos a cada 6 horas se chiado.",
             orient="Evitar poeira e fumaça; retorno em 15 dias.", exames="Não se aplica.", tuss="10101012 - Consulta em consultório", procs=[]),
        dict(queixa="Puericultura - acompanhamento do crescimento.", anamnese="Desenvolvimento neuropsicomotor adequado para a idade. Vacinação em dia.",
             diagnostico="Criança saudável.", cid="Z00.1 - Exame de rotina de saúde da criança",
             tit="Suplementação", presc="Vitamina D 400 UI - 1 gota ao dia.", orient="Alimentação variada, manter calendário vacinal.",
             exames="Hemograma; Ferritina.", tuss="10101012 - Consulta em consultório", procs=[]),
    ],
    "ORTO": [
        dict(queixa="Dor lombar há 3 semanas após carregar peso.", anamnese="Dor em queimação na região lombar, sem irradiação para membros. Trabalha sentado.",
             diagnostico="Lombalgia mecânica aguda.", cid="M54.5 - Dor lombar baixa",
             tit="Analgésico e relaxante", presc="Ciclobenzaprina 5 mg - 1 comprimido à noite por 7 dias.",
             orient="Fisioterapia 10 sessões, correção postural.", exames="Radiografia de coluna lombossacra.", tuss="10101012 - Consulta em consultório", procs=[]),
        dict(queixa="Dor no joelho direito ao subir escadas.", anamnese="Dor anterior no joelho, piora com agachamento. Pratica corrida.",
             diagnostico="Condromalácia patelar.", cid="M22.4 - Condromalácia da rótula",
             tit="Anti-inflamatório", presc="Nimesulida 100 mg - 1 comprimido a cada 12 horas por 5 dias.",
             orient="Fortalecimento de quadríceps, gelo 20 minutos após atividade.", exames="Ressonância magnética do joelho direito.", tuss="10101012 - Consulta em consultório",
             procs=[("Infiltração articular de joelho", 600.00)]),
    ],
    "MED_ENDOCRINOLOGIA": [
        dict(queixa="Ganho de peso e queda de cabelo.", anamnese="Cansaço, intolerância ao frio e constipação há 4 meses.",
             diagnostico="Hipotireoidismo primário.", cid="E03.9 - Hipotireoidismo não especificado",
             tit="Reposição hormonal", presc="Levotiroxina 50 mcg - 1 comprimido em jejum, 30 minutos antes do café.",
             orient="Tomar sempre no mesmo horário; repetir TSH em 6 semanas.", exames="TSH; T4 livre; Anti-TPO.", tuss="10101012 - Consulta em consultório", procs=[]),
        dict(queixa="Avaliação de obesidade.", anamnese="IMC 33. Tentativas prévias de dieta sem sucesso. Ronco noturno.",
             diagnostico="Obesidade grau I com resistência insulínica.", cid="E66.9 - Obesidade não especificada",
             tit="Orientação nutricional", presc="Plano alimentar hipocalórico; Metformina 500 mg após jantar.",
             orient="Encaminhamento para nutricionista; atividade física 150 minutos por semana.", exames="Insulina; Glicemia; Perfil lipídico; TSH.", tuss="10101012 - Consulta em consultório", procs=[]),
    ],
    "MED_OTORRINOLARINGOLOGIA": [
        dict(queixa="Obstrução nasal e espirros frequentes.", anamnese="Sintomas pioram pela manhã e em contato com poeira.",
             diagnostico="Rinite alérgica persistente.", cid="J30.4 - Rinite alérgica não especificada",
             tit="Corticoide nasal", presc="Budesonida spray nasal 64 mcg - 2 jatos em cada narina pela manhã por 30 dias.",
             orient="Lavagem nasal com soro fisiológico 2x ao dia.", exames="Nasofibroscopia.", tuss="10101012 - Consulta em consultório",
             procs=[("Nasofibroscopia", 250.00)]),
        dict(queixa="Dor de ouvido esquerdo há 2 dias.", anamnese="Otalgia após banho de piscina, sem febre.",
             diagnostico="Otite externa aguda.", cid="H60.9 - Otite externa não especificada",
             tit="Antibiótico tópico", presc="Ciprofloxacino + hidrocortisona gotas otológicas - 3 gotas 2x ao dia por 7 dias.",
             orient="Evitar molhar o ouvido por 10 dias.", exames="Não se aplica.", tuss="10101012 - Consulta em consultório", procs=[]),
    ],
}

CASOS_DENTISTA = [
    dict(queixa="Dor ao mastigar do lado direito.", anamnese="Sensibilidade a doces e frio no dente 16 há 1 mês.", diagnostico="Cárie de dentina no dente 16.",
         cid="K02.1 - Cárie da dentina", plano="Restauração em resina composta no 16; profilaxia.", procedimentos="Anestesia local; remoção de tecido cariado; restauração classe II em resina.",
         tuss="85100196 - Restauração em resina fotopolimerizável 2 faces", dentes=[(16, "cariado", "Cárie oclusal-mesial"), (26, "obturado", "Restauração antiga em amálgama")],
         procs=[("Restauração em resina composta - dente 16", 250.00), ("Profilaxia e polimento coronário", 180.00)]),
    dict(queixa="Dor latejante intensa que piora à noite.", anamnese="Dor espontânea no dente 36 há 4 dias, uso de analgésico sem melhora.", diagnostico="Pulpite irreversível no dente 36.",
         cid="K04.0 - Pulpite", plano="Tratamento endodôntico do 36 e coroa provisória.", procedimentos="Abertura coronária, pulpectomia e medicação intracanal.",
         tuss="85200034 - Tratamento endodôntico multirradicular", dentes=[(36, "canal", "Endodontia em andamento"), (46, "obturado", None)],
         procs=[("Tratamento de canal - molar 36", 900.00), ("Coroa provisória - dente 36", 250.00)]),
    dict(queixa="Gengiva sangra ao escovar.", anamnese="Sangramento gengival há 3 meses, mau hálito. Fumante.", diagnostico="Periodontite crônica localizada.",
         cid="K05.3 - Periodontite crônica", plano="Raspagem e alisamento radicular por quadrante; reavaliação em 30 dias.", procedimentos="Raspagem supragengival e subgengival nos quadrantes 3 e 4.",
         tuss="85300039 - Raspagem subgengival e alisamento radicular", dentes=[(31, "sadio", "Mobilidade grau I"), (41, "sadio", "Bolsa de 5 mm")],
         procs=[("Raspagem periodontal - quadrante 3", 400.00), ("Raspagem periodontal - quadrante 4", 400.00)]),
    dict(queixa="Falta de um dente e dificuldade para mastigar.", anamnese="Extração do 46 há 2 anos. Boa saúde geral.", diagnostico="Ausência do elemento 46 com bom volume ósseo.",
         cid="K08.1 - Perda de dentes devida a acidente, extração ou doença periodontal local", plano="Implante osseointegrado no 46 e coroa sobre implante após 4 meses.",
         procedimentos="Planejamento com tomografia; instalação de implante.", tuss="85400017 - Implante osseointegrado",
         dentes=[(46, "ausente", "Indicado implante"), (47, "obturado", None)],
         procs=[("Implante dentário osseointegrado - região 46", 3500.00), ("Coroa sobre implante - 46", 1800.00)]),
    dict(queixa="Dentes tortos e incômodo estético.", anamnese="Apinhamento anteroinferior, respiração bucal na infância.", diagnostico="Má oclusão classe II com apinhamento.",
         cid="K07.3 - Anomalias da posição dos dentes", plano="Aparelho ortodôntico fixo com manutenção mensal.", procedimentos="Colagem de braquetes e instalação de arco inicial.",
         tuss="85500011 - Aparelho ortodôntico fixo", dentes=[(32, "sadio", "Giroversão"), (42, "sadio", "Apinhamento")],
         procs=[("Instalação de aparelho ortodôntico fixo", 1200.00), ("Manutenção ortodôntica mensal", 220.00)]),
    dict(queixa="Avaliação e limpeza de rotina.", anamnese="Sem queixas. Última consulta há 1 ano.", diagnostico="Gengivite leve associada a biofilme.",
         cid="K05.1 - Gengivite crônica", plano="Profilaxia, aplicação de flúor e orientação de higiene.", procedimentos="Profilaxia com jato de bicarbonato e aplicação tópica de flúor.",
         tuss="85100048 - Profilaxia: polimento coronário", dentes=[(11, "sadio", None), (21, "sadio", None)],
         procs=[("Profilaxia e polimento coronário", 180.00), ("Aplicação tópica de flúor", 90.00)]),
    dict(queixa="Dente da frente quebrado após queda.", anamnese="Fratura do 21 há 2 dias, sem dor espontânea.", diagnostico="Fratura coronária de esmalte e dentina no 21.",
         cid="S02.5 - Fratura de dentes", plano="Restauração estética do 21; acompanhamento da vitalidade pulpar.", procedimentos="Reconstrução em resina com guia palatina.",
         tuss="85100200 - Restauração em resina fotopolimerizável 3 faces", dentes=[(21, "fratura", "Fratura do ângulo incisal"), (11, "sadio", None)],
         procs=[("Restauração estética em resina - dente 21", 320.00)]),
    dict(queixa="Prótese antiga machucando.", anamnese="Usa prótese parcial removível superior há 10 anos.", diagnostico="Prótese parcial removível desadaptada.",
         cid="K08.8 - Outros transtornos especificados dos dentes e estruturas de sustentação", plano="Confecção de nova prótese parcial removível superior.",
         procedimentos="Moldagem anatômica e funcional.", tuss="85600017 - Prótese parcial removível", dentes=[(14, "protese", None), (15, "protese", None), (24, "ausente", None)],
         procs=[("Prótese parcial removível superior", 1500.00)]),
]

PROCEDIMENTOS_PADRAO = {
    "MEDICO": [("Consulta de retorno", 0.00), ("Eletrocardiograma de repouso", 120.00), ("Exérese de lesão de pele com anatomopatológico", 450.00),
               ("Infiltração articular de joelho", 600.00), ("Nasofibroscopia", 250.00), ("Coleta de colpocitologia oncótica", 90.00), ("Teste ergométrico", 280.00)],
    "DENTISTA": [("Profilaxia e polimento coronário", 180.00), ("Restauração em resina composta", 250.00), ("Tratamento de canal", 900.00),
                 ("Extração simples", 300.00), ("Clareamento dental de consultório", 800.00), ("Implante dentário osseointegrado", 3500.00),
                 ("Aplicação tópica de flúor", 90.00), ("Raspagem periodontal por quadrante", 400.00)],
}


# --------------------------------------------------------------------------------------
# Utilidades
# --------------------------------------------------------------------------------------
def sql_str(valor):
    """Converte um valor Python em literal SQL (None -> NULL). Expressões SQL usam a classe Sql."""
    if valor is None:
        return "NULL"
    if isinstance(valor, Sql):
        return valor.expr
    if isinstance(valor, bool):
        return "1" if valor else "0"
    if isinstance(valor, (int, float)):
        return f"{valor:.2f}" if isinstance(valor, float) else str(valor)
    return "'" + str(valor).replace("\\", "\\\\").replace("'", "''") + "'"


class Sql:
    """Marca um trecho como expressão SQL crua (não vira string)."""

    def __init__(self, expr):
        self.expr = expr


def dia(idx):
    """Variável SQL com a data do idx-ésimo dia útil (0 = hoje ou próximo dia útil)."""
    return f"@d_{'m' if idx < 0 else 'p'}{abs(idx)}"


def ts(idx, minutos, delta_min=0):
    """Expressão DATETIME: dia útil idx no horário (minutos desde 00:00) + delta."""
    total = minutos + delta_min
    base = f"TIMESTAMP({dia(idx)}, '{total // 60:02d}:{total % 60:02d}:00')" if 0 <= total < 1440 else None
    if base is None:
        return f"(TIMESTAMP({dia(idx)}, '00:00:00') + INTERVAL {total} MINUTE)"
    return base


def cpf_valido(rng, usados):
    """Gera CPF com dígitos verificadores corretos, único no conjunto 'usados'."""
    while True:
        base = [rng.randint(0, 9) for _ in range(9)]
        if len(set(base)) == 1:
            continue
        for tamanho in (9, 10):
            soma = sum(d * (tamanho + 1 - i) for i, d in enumerate(base[:tamanho]))
            dv = (soma * 10) % 11
            base.append(0 if dv == 10 else dv)
        cpf = "".join(map(str, base))
        if cpf != LOGIN_ROOT and cpf not in usados:
            usados.add(cpf)
            return cpf


def cnpj_valido(rng, usados):
    """Gera CNPJ (matriz 0001) com dígitos verificadores corretos e único."""
    while True:
        base = [rng.randint(0, 9) for _ in range(8)] + [0, 0, 0, 1]
        for pesos in ([5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2], [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]):
            resto = sum(d * p for d, p in zip(base, pesos)) % 11
            base.append(0 if resto < 2 else 11 - resto)
        cnpj = "".join(map(str, base))
        if cnpj not in usados:
            usados.add(cnpj)
            return cnpj


def hash_senha(rng, senha):
    """BCrypt $2a$10$ com salt derivado da seed: mesma seed -> mesmo .sql (diff limpo)."""
    alfabeto = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    salt = "$2a$10$" + "".join(rng.choice(alfabeto) for _ in range(22))
    return bcrypt.hashpw(senha.encode(), salt.encode()).decode()


def sem_acento(texto):
    tabela = str.maketrans("áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ", "aaaaaeeeeiiiiooooouuuucAAAAAEEEEIIIIOOOOOUUUUC")
    return texto.translate(tabela)


class Gerador:
    """Monta as linhas de cada tabela em memória e escreve o .sql no fim."""

    def __init__(self, seed):
        self.rng = random.Random(seed)
        self.seed = seed
        self.cpfs = set()
        self.cnpjs = set()
        self.emails = set()
        self.rows = {}
        self.ids = {}
        self.logins = []  # (tenant, perfil, nome, login)
        # prontuario e prontuario_dentista começam do 1 e o GET /planejamento-terapeutico/prontuario/{id}
        # procura o id primeiro como prontuário de dentista e só depois como médico: com ids iguais nas
        # duas tabelas o médico enxergaria o planejamento do dentista. Faixa separada evita a colisão.
        self.ids["prontuario_dentista"] = 100000
        self.cobrancas = []  # (assinatura_id, organizacao_id, valor, variável de início)

    # ----------------------------------------------------------------- helpers
    def novo_id(self, tabela):
        self.ids[tabela] = self.ids.get(tabela, 0) + 1
        return self.ids[tabela]

    def add(self, tabela, colunas, valores):
        destino = self.rows.setdefault(tabela, (colunas, []))
        assert destino[0] == colunas, tabela
        destino[1].append("(" + ", ".join(sql_str(v) for v in valores) + ")")

    def uuid(self):
        return str(uuid.UUID(int=self.rng.getrandbits(128), version=4))

    def nome(self, sexo):
        primeiro = self.rng.choice(NOMES_M if sexo == "M" else NOMES_F)
        return f"{primeiro} {self.rng.choice(SOBRENOMES)} {self.rng.choice(SOBRENOMES)}"

    def email(self, nome, dominio="example.com"):
        partes = sem_acento(nome).lower().split()
        base = f"{partes[0]}.{partes[-1]}"
        email, n = f"{base}@{dominio}", 1
        while email in self.emails:
            n += 1
            email = f"{base}{n}@{dominio}"
        self.emails.add(email)
        return email

    def telefone(self, cidade):
        return CIDADES[cidade]["ddd"] + "9" + str(self.rng.randint(10_000_000, 99_999_999))

    def endereco(self, cidade, comercial=False):
        info = CIDADES[cidade]
        eid = self.novo_id("endereco")
        cep = f"{self.rng.randint(*info['cep']):05d}{self.rng.randint(0, 999):03d}"
        compl = self.rng.choice(["Sala 101", "Sala 304", "Conjunto 52"]) if comercial else self.rng.choice([None, None, "Apto 12", "Apto 83", "Casa 2", "Bloco B"])
        self.add("endereco", "EndCodigo, EndNacionalidade, EndUF, EndMunicipio, EndBairro, EndCep, EndRua, EndNumero, EndComplemento",
                 [eid, "Brasileira", info["uf"], cidade, self.rng.choice(info["bairros"]), cep, self.rng.choice(RUAS),
                  self.rng.randint(10, 3500), compl])
        return eid

    def nascimento(self, idade_min, idade_max):
        dias = self.rng.randint(idade_min * 365 + 10, idade_max * 365 + 300)
        return Sql(f"DATE_SUB(@hoje, INTERVAL {dias} DAY)")

    def usuario(self, org_id, login, perfil, email):
        uid = self.novo_id("usuarios")
        self.add("usuarios", "id, organizacao_id, login, senha, tipo_usuario_novo, status, email, motivo_bloqueio",
                 [uid, org_id, login, hash_senha(self.rng, login), perfil, "ATIVO", email, None])
        return uid

    def mensagem(self, org_id, entidade_id, email, nome, assunto, corpo, tipo, data_expr, status="ENVIADO", erro=None,
                 tentativas=1, admin_notificado=False):
        mid = self.novo_id("mensageria")
        self.add("mensageria", "id, organizacao_id, destinatario_entidade_id, destinatario_email, destinatario_nome, assunto, corpo_mensagem, "
                 "tipo_mensagem, status, erro_detalhe, tentativas, admin_notificado, data_criacao, data_atualizacao",
                 [mid, org_id, entidade_id, email, nome, assunto, corpo, tipo, status, erro, tentativas,
                  Sql("b'1'" if admin_notificado else "b'0'"), Sql(data_expr), Sql(f"({data_expr} + INTERVAL 1 MINUTE)")])

    # ----------------------------------------------------------------- tenants
    def gerar(self):
        self.usuario(None, LOGIN_ROOT, "ROOT", EMAIL_ROOT)
        self.logins.append(("Plataforma", "ROOT (SUPER_ADMIN)", "Super Administrador", LOGIN_ROOT))
        for t in TENANTS:
            self.gerar_tenant(t)

    def gerar_tenant(self, t):
        rng = self.rng
        cidade = t["cidade"]
        org_id = self.novo_id("organizacao")
        cnpj = cnpj_valido(rng, self.cnpjs)
        org_criada = f"(TIMESTAMP(@ini_{t['chave']}, '09:00:00') - INTERVAL 7 DAY)"
        slug = sem_acento(t["nome"]).lower().replace("&", "e").replace(" ", "")
        email_org = f"contato.{slug}@example.com"
        self.emails.add(email_org)
        end_org = self.endereco(cidade, comercial=True)
        self.add("organizacao", "id, nome, razao_social, cnpj, tipo, email, telefone, logo_url, endereco_id, status, created_at, updated_at",
                 [org_id, t["nome"], t["razao"], cnpj, t["tipo"], email_org, self.telefone(cidade), None, end_org, "ATIVO",
                  Sql(org_criada), Sql(org_criada)])

        # Gestores: o dono (is_owner) entra com o CNPJ, como no cadastro de tenant Pessoa Jurídica.
        gestores = []
        for i in range(t["gestores"]):
            sexo = rng.choice("MF")
            nome = self.nome(sexo)
            email = self.email(nome)
            login = cnpj if i == 0 else cpf_valido(rng, self.cpfs)
            uid = self.usuario(org_id, login, "GESTOR", email)
            aid = self.novo_id("admin_organizacao")
            self.add("admin_organizacao", "id, organizacao_id, usuario_id, nome, cargo, email, is_owner, permissoes, status, created_at",
                     [aid, org_id, uid, nome, "Administrador" if i == 0 else "Gestor", email, 1 if i == 0 else 0, None, "ATIVO", Sql(org_criada)])
            gestores.append(dict(id=aid, uid=uid, nome=nome, email=email))
            self.logins.append((t["nome"], "GESTOR (dono, login=CNPJ)" if i == 0 else "GESTOR", nome, login))
            self.mensagem(org_id, aid, email, nome, "Bem-vindo ao SaúdeConecta - suas credenciais de acesso",
                          f"Olá {nome}, o acesso de administrador da {t['nome']} foi criado. Login: {login}.",
                          "EMAIL_CREDENCIAIS_ADMINISTRADOR", f"({org_criada} + INTERVAL {5 + i * 30} MINUTE)")

        secretarias = []
        for i in range(t["secretarias"]):
            nome = self.nome("F" if rng.random() < 0.85 else "M")
            email = self.email(nome)
            cpf = cpf_valido(rng, self.cpfs)
            uid = self.usuario(org_id, cpf, "ASSISTENTE", email)
            sid = self.novo_id("secretaria")
            self.add("secretaria", "id, organizacao_id, usuario_id, nome, cpf, email, telefone, status",
                     [sid, org_id, uid, nome, cpf, email, self.telefone(cidade), "ATIVO"])
            secretarias.append(dict(id=sid, uid=uid, nome=nome, email=email))
            self.logins.append((t["nome"], "ASSISTENTE (secretária)", nome, cpf))
            self.mensagem(org_id, sid, email, nome, "Suas credenciais de acesso - SaúdeConecta",
                          f"Olá {nome}, seu acesso de secretária na {t['nome']} foi criado. Login: {cpf}.",
                          "EMAIL_CREDENCIAIS_SECRETARIA", f"({org_criada} + INTERVAL {1 + i} DAY)")

        profissionais = []
        for tipo, especialidades in (("MEDICO", t["medicos"]), ("DENTISTA", t["dentistas"])):
            for esp in especialidades:
                sexo = "F" if esp == "GINE" and rng.random() < 0.8 else rng.choice("MF")
                prefixo = ("Dra. " if sexo == "F" else "Dr. ")
                nome = prefixo + self.nome(sexo)
                email = self.email(nome.split(" ", 1)[1])
                cpf = cpf_valido(rng, self.cpfs)
                uid = self.usuario(org_id, cpf, "CLINICO", email)
                pid = self.novo_id("profissional")
                conselho = "CRM" if tipo == "MEDICO" else "CRO"
                duracao = rng.choice([30, 30, 40, 45, 60]) if tipo == "MEDICO" else rng.choice([30, 45, 60, 60])
                valor = float(rng.choice([250, 280, 300, 350, 400, 450]) if tipo == "MEDICO" else rng.choice([150, 180, 200, 250, 300]))
                self.add("profissional", "id, organizacao_id, tipo_profissional_id, nome, sexo, data_nascimento, registro_conselho, cpf, rg, email, telefone, "
                         "formacao, instituicao, tempo_consulta_minutos, valor_consulta, usuario_id, endereco_id, status, created_at, updated_at",
                         [pid, org_id, Sql(f"@tp_{tipo}"), nome, "FEMININO" if sexo == "F" else "MASCULINO", self.nascimento(29, 64),
                          f"{conselho}-{CIDADES[cidade]['uf']} {rng.randint(10000, 199999)}", cpf, str(rng.randint(10_000_000, 59_999_999)),
                          email, self.telefone(cidade), "Medicina" if tipo == "MEDICO" else "Odontologia", rng.choice(INSTITUICOES_MED),
                          duracao, valor, uid, self.endereco(cidade), "ATIVO", Sql(org_criada), Sql(org_criada)])
                self.add("profissional_especialidade", "profissional_id, especialidade_id, principal", [pid, Sql(f"@esp_{esp}"), 1])
                for proc, valor_proc in rng.sample(PROCEDIMENTOS_PADRAO[tipo], 5):
                    self.add("tb_procedimento_padrao", "id, profissional_id, organizacao_id, nome_procedimento, valor_padrao, ativo, created_at, updated_at",
                             [self.novo_id("tb_procedimento_padrao"), pid, org_id, proc, valor_proc, Sql("b'1'"), Sql(org_criada), Sql(org_criada)])
                profissionais.append(dict(id=pid, uid=uid, nome=nome, email=email, tipo=tipo, esp=esp, duracao=duracao, valor=valor))
                self.logins.append((t["nome"], f"CLINICO ({'médico' if tipo == 'MEDICO' else 'dentista'} - {esp})", nome, cpf))
                self.mensagem(org_id, pid, email, nome, "Suas credenciais de acesso - SaúdeConecta",
                              f"Olá {nome}, seu acesso de clínico na {t['nome']} foi criado. Login: {cpf}.",
                              "EMAIL_CREDENCIAIS_CLINICO", f"({org_criada} + INTERVAL {2 + len(profissionais)} DAY)")

        self.gerar_assinatura(t, org_id)
        self.add("configuracoes_consulta", "id, organizacao_id, pular_para_confirmado, descricao, created_at, updated_at",
                 [self.novo_id("configuracoes_consulta"), org_id, 1 if t["pular_confirmado"] else 0,
                  "Fluxo acelerado: consultas já nascem confirmadas" if t["pular_confirmado"] else "Configuração padrão - fluxo normal",
                  Sql(org_criada), Sql(org_criada)])

        for prof in profissionais:
            self.gerar_agenda(t, org_id, cidade, prof, secretarias, gestores)

        # Pacientes inativos (cadastro antigo, sem agenda) para a tela de filtro ATIVO/INATIVO.
        for _ in range(2):
            self.paciente(org_id, cidade, 20, 70, status="INATIVO")

        self.gerar_chamados(t, org_id, gestores)

    def gerar_assinatura(self, t, org_id):
        plano = PLANOS[t["plano"]]
        limites = t["custom"] or (None, None, None)
        extras = [max(0, c - p) if c is not None else 0 for c, p in zip(limites, plano["limites"])]
        adicional = round(sum(e * v for e, v in zip(extras, VALOR_EXTRA)), 2)
        valor = round(plano["valor"] + adicional, 2)
        aid = self.novo_id("assinatura_tenant")
        ini = f"@ini_{t['chave']}"
        venc = f"DATE_ADD({ini}, INTERVAL (TIMESTAMPDIFF(MONTH, {ini}, @hoje) + 1) MONTH)"
        if t["cortesia"]:
            venc_expr, prox = "DATE_ADD(@hoje, INTERVAL 365 DAY)", None
        else:
            venc_expr, prox = venc, Sql(venc)
        self.add("assinatura_tenant", "id, organizacao_id, plano_assinatura_id, status, data_inicio, data_vencimento, data_proxima_cobranca, valor_mensal, "
                 "criado_em, atualizado_em, limite_admin_org_custom, limite_profissional_custom, limite_secretaria_custom, qtd_admin_extra, "
                 "qtd_profissional_extra, qtd_secretaria_extra, valor_adicional_perfis, mp_preapproval_id, mp_status, mp_init_point, mp_payer_email, "
                 "mp_ultima_sincronizacao, cortesia, data_cancelamento, cancelado_por, data_suspensao, dia_cobranca, versao",
                 [aid, org_id, Sql(f"@plano_{t['plano']}"), "ATIVA", Sql(ini), Sql(venc_expr), prox, valor,
                  Sql(f"(TIMESTAMP({ini}, '09:00:00') - INTERVAL 7 DAY)"), Sql("@agora"), limites[0], limites[1], limites[2],
                  extras[0], extras[1], extras[2], adicional, None, None, None, None, None, 1 if t["cortesia"] else 0,
                  None, None, None, None if t["cortesia"] else Sql(f"DAY({ini})"), 0])
        if not t["cortesia"]:
            self.cobrancas.append((aid, org_id, valor, ini))

    # ----------------------------------------------------------------- pacientes e agenda
    def paciente(self, org_id, cidade, idade_min, idade_max, sexo=None, status="ATIVO"):
        sexo = sexo or self.rng.choice("MF")
        nome = self.nome(sexo)
        pid = self.novo_id("paciente")
        email = self.email(nome)
        self.add("paciente", "PaciCodigo, PaciNome, PaciSexo, PaciDataNacimento, PaciCpf, PaciRg, PaciEmail, PaciTelefone, Endereco, PaciStatus, organizacao_id",
                 [pid, nome, "1" if sexo == "M" else "2", self.nascimento(idade_min, idade_max), cpf_valido(self.rng, self.cpfs),
                  str(self.rng.randint(10_000_000, 59_999_999)), email, self.telefone(cidade), self.endereco(cidade), status, org_id])
        return dict(id=pid, nome=nome, email=email, sexo=sexo)

    def gerar_agenda(self, t, org_id, cidade, prof, secretarias, gestores):
        rng = self.rng
        # Pool de pacientes do profissional (disjunto entre profissionais: evita o mesmo paciente
        # em dois consultórios no mesmo horário). Pesos decrescentes = pacientes recorrentes.
        if prof["esp"] == "PED":
            faixa, sexo = (1, 13), None
        elif prof["esp"] == "GINE":
            faixa, sexo = (18, 65), "F"
        elif prof["esp"] == "ORTODONT":
            faixa, sexo = (11, 40), None
        else:
            faixa, sexo = (18, 85), None
        pool = []
        for _ in range(38):
            pac = self.paciente(org_id, cidade, *faixa, sexo=sexo)
            if prof["tipo"] == "MEDICO":
                pac["caso"] = rng.choice(CASOS_MEDICO[prof["esp"]])
            else:
                pac["caso"] = rng.choice(CASOS_DENTISTA)
            pac["responsavel"] = self.nome(rng.choice("MF")) if faixa[1] <= 13 else None
            pac["visitas"] = 0
            pac["tem_termo"] = False
            pool.append(pac)
        pesos = [1 / (i + 1) ** 0.75 for i in range(len(pool))]
        criadores = [s["uid"] for s in secretarias] + [g["uid"] for g in gestores[:1]]
        dur = prof["duracao"]
        janelas = [(8 * 60, 12 * 60), (13 * 60 + 30, 18 * 60)]

        for idx in range(-DIAS_UTEIS, DIAS_UTEIS + 1):
            prob = 0.55 if idx <= 0 else max(0.25, 0.55 - 0.012 * idx)
            usados_no_dia = set()
            for inicio, fim in janelas:
                minuto = inicio
                while minuto + dur <= fim:
                    if rng.random() < prob:
                        pac = None
                        for _ in range(10):
                            cand = rng.choices(pool, weights=pesos)[0]
                            if cand["id"] not in usados_no_dia:
                                pac = cand
                                break
                        if pac:
                            usados_no_dia.add(pac["id"])
                            self.consulta(t, org_id, prof, pac, idx, minuto, rng.choice(criadores), secretarias)
                    minuto += dur

    def consulta(self, t, org_id, prof, pac, idx, minuto, criado_por, secretarias):
        rng = self.rng
        cid = self.novo_id("consulta")
        dur = prof["duracao"]
        inicio = ts(idx, minuto)
        fim_min = minuto + dur
        forma = rng.choices([f for f, _ in FORMAS_PAGAMENTO], weights=[p for _, p in FORMAS_PAGAMENTO])[0]
        valor = round(prof["valor"] * (0.6 if forma == "CONVENIO" else 1.0), 2)
        dias_antes = rng.randint(2, 20)
        criado_expr = f"(TIMESTAMP({dia(idx)}, '10:00:00') - INTERVAL {dias_antes} DAY)" if idx <= 0 else f"(@agora - INTERVAL {dias_antes} DAY)"
        inicial = "CONFIRMADA" if t["pular_confirmado"] else "AGENDADA"
        hist = [(None, inicial, "Consulta cadastrada com fluxo acelerado (status CONFIRMADA)" if t["pular_confirmado"] else "Consulta cadastrada",
                 criado_por, criado_expr)]
        secretaria_uid = rng.choice(secretarias)["uid"]
        cancelado_por = motivo = None

        def confirmar(expr):
            if hist[-1][1] == "AGENDADA":
                hist.append(("AGENDADA", "CONFIRMADA", "Consulta confirmada", secretaria_uid, expr))

        if idx < 0:
            if rng.random() < 0.09:
                status = "CANCELADA"
                cancelado_por, motivo = rng.choice([
                    ("PACIENTE", "Paciente não compareceu (falta)"), ("PACIENTE", "Paciente não compareceu (falta)"),
                    ("PACIENTE", "Paciente solicitou remarcação"), ("PROFISSIONAL", "Imprevisto na agenda do profissional"),
                    ("CLINICA", "Clínica remarcou por manutenção da sala")])
                if rng.random() < 0.6:
                    confirmar(f"({criado_expr} + INTERVAL 3 HOUR)")
                quando = ts(idx, minuto, 20) if motivo.startswith("Paciente não compareceu") else f"({inicio} - INTERVAL 1 DAY)"
                hist.append((hist[-1][1], "CANCELADA", f"Consulta cancelada: {motivo}", secretaria_uid, quando))
            else:
                if rng.random() < 0.85:
                    confirmar(f"({inicio} - INTERVAL 1 DAY)")
                hist.append((hist[-1][1], "REALIZADA", "Consulta concluída", prof["uid"], ts(idx, fim_min)))
                pago = rng.random() < (0.65 if idx >= -3 else 0.96)
                status = "PAGO" if pago else "REALIZADA"
                if pago:
                    hist.append(("REALIZADA", "PAGO", "Consulta marcada como paga", secretaria_uid, ts(idx, fim_min, 10)))
        elif idx == 0:
            status = "CONFIRMADA" if rng.random() < 0.6 else inicial
            if status == "CONFIRMADA":
                confirmar(f"({criado_expr} + INTERVAL 1 DAY)")
        else:
            if rng.random() < 0.04:
                status = "CANCELADA"
                cancelado_por, motivo = "PACIENTE", rng.choice(["Paciente solicitou remarcação", "Paciente viajará na data"])
                hist.append((hist[-1][1], "CANCELADA", f"Consulta cancelada: {motivo}", secretaria_uid, f"({criado_expr} + INTERVAL 1 DAY)"))
            else:
                status = "CONFIRMADA" if rng.random() < (0.7 if idx <= 2 else 0.25) else inicial
                if status == "CONFIRMADA":
                    confirmar(f"({criado_expr} + INTERVAL 2 HOUR)")

        obs = rng.choice([None, None, "Primeira consulta" if pac["visitas"] == 0 else "Retorno", "Paciente prefere contato por WhatsApp",
                          "Trazer exames anteriores"])
        self.add("consulta", "id, organizacao_id, profissional_id, paciente_id, especialidade_id, data_hora, duracao_minutos, observacoes, "
                 "forma_pagamento_id, valor, status, cancelado_por, motivo_cancelamento, criado_por, created_at, updated_at",
                 [cid, org_id, prof["id"], pac["id"], Sql(f"@esp_{prof['esp']}"), Sql(inicio), dur, obs, Sql(f"@fp_{forma}"), valor,
                  status, cancelado_por, motivo, criado_por, Sql(criado_expr), Sql(hist[-1][4])])
        for anterior, novo, texto, autor, quando in hist:
            self.add("consulta_historico", "id, consulta_id, status_anterior, status_novo, observacao, alterado_por, created_at",
                     [self.novo_id("consulta_historico"), cid, anterior, novo, texto, autor, Sql(quando)])

        realizada = status in ("REALIZADA", "PAGO")
        if realizada:
            prontuario_id = self.prontuario(org_id, prof, pac, cid, idx, minuto)
            if pac["visitas"] == 0 and pac["caso"]["procs"]:
                self.planejamento(org_id, prof, pac, cid, idx, fim_min, prontuario_id)
            if not pac["tem_termo"]:
                self.termo_assinado(org_id, pac, cid, idx, minuto)
            pac["visitas"] += 1
        elif status != "CANCELADA" and 0 <= idx <= 2 and not pac["tem_termo"] and rng.random() < 0.5:
            self.termo_pendente(org_id, pac, cid)

        # Lembretes enviados aos pacientes (mensageria).
        if status != "CANCELADA" and -8 <= idx <= 2 and rng.random() < 0.7:
            envio = f"({inicio} - INTERVAL 1 DAY)" if idx <= 0 else f"(@agora - INTERVAL {rng.randint(1, 5)} HOUR)"
            corpo = Sql("CONCAT(" + sql_str(f"Olá {pac['nome']}, lembramos da sua consulta com {prof['nome']} na {t['nome']} em ")
                        + f", DATE_FORMAT({dia(idx)}, '%d/%m/%Y'), " + sql_str(f" às {minuto // 60:02d}:{minuto % 60:02d}. Responda para confirmar.") + ")")
            falhou = rng.random() < 0.03
            self.mensagem(org_id, pac["id"], pac["email"], pac["nome"], f"Lembrete de consulta - {t['nome']}", corpo, "EMAIL_GENERICO", envio,
                          status="FALHOU" if falhou else "ENVIADO",
                          erro="550 5.1.1 Mailbox unavailable: caixa de destino inexistente" if falhou else None,
                          tentativas=3 if falhou else 1)

    def prontuario(self, org_id, prof, pac, consulta_id, idx, minuto):
        rng = self.rng
        caso = pac["caso"]
        retorno = pac["visitas"] > 0
        tempo = f"{max(8, prof['duracao'] - rng.randint(0, 15))}:{rng.randint(0, 59):02d}"
        if prof["tipo"] == "MEDICO":
            pid = self.novo_id("prontuario")
            crianca = pac["responsavel"] is not None
            peso = round(rng.uniform(10, 45), 1) if crianca else round(rng.uniform(50, 105), 1)
            altura = round(rng.uniform(0.8, 1.55), 2) if crianca else round(rng.uniform(1.50, 1.92), 2)
            sist, diast = rng.randint(105, 150), rng.randint(65, 95)
            queixa = ("Retorno: " + caso["queixa"].lower()) if retorno else caso["queixa"]
            anamnese = (f"Paciente em acompanhamento. Refere melhora parcial após conduta da consulta anterior. {caso['anamnese']}"
                        if retorno else caso["anamnese"])
            self.add("prontuario", "prontCodigoProntuario, prontPeso, prontAltura, prontTemperatura, prontSaturacao, prontHemoglobina, prontPressao, "
                     "prontFrequenciaRespiratoria, prontFrequenciaArterialSistolica, prontFrequenciaArterialDiastolica, prontObservacao, prontAnamnese, "
                     "prontQueixaPricipal, prontDiagnostico, prontTituloPrescricao, prontPrescricao, prontDataFinalizado, prontCodigoMedico, consulta, "
                     "prontTempoDuracao, prontResponsavel, prontPulso, prontOrientacoes, prontTussTexto, prontCidTexto, prontSolicitacaoExameTexto",
                     [pid, str(peso), str(altura), f"{rng.uniform(36.0, 37.6):.1f}", str(rng.randint(95, 99)), f"{rng.uniform(11.5, 15.5):.1f}",
                      f"{sist}/{diast}", str(rng.randint(12, 20)), str(sist), str(diast),
                      "Paciente orientado(a) e de acordo com a conduta." if not retorno else "Evolução satisfatória.",
                      anamnese, queixa, caso["diagnostico"], caso["tit"], caso["presc"], Sql(dia(idx)), prof["id"], consulta_id,
                      tempo, pac["responsavel"], str(rng.randint(60, 98)), caso["orient"], caso["tuss"], caso["cid"], caso["exames"]])
            return ("medico", pid)
        pid = self.novo_id("prontuario_dentista")
        self.add("prontuario_dentista", "prontdent_codigo, prontdent_queixa_principal, prontdent_anamnese, prontdent_observacao, prontdent_higiene_bucal, "
                 "prontdent_condicao_gengival, prontdent_oclusal, prontdent_atm, prontdent_labios, prontdent_mucosas, prontdent_soalho_bucal, prontdent_palato, "
                 "prontdent_orofaringe, prontdent_lingua, prontdent_gengiva, prontdent_habitos_nocivos, prontdent_portador_aparelho, prontdent_exame_outros, "
                 "prontdent_pressao_arterial, prontdent_pulso, prontdent_altura, prontdent_temperatura, prontdent_peso, prontdent_edema, prontdent_facies, "
                 "prontdent_linfonodos, prontdent_diagnostico, prontdent_plano_tratamento, prontdent_titulo_prescricao, prontdent_data_prescricao, "
                 "prontdent_prescricao, prontdent_titulo_exame, prontdent_data_exame, prontdent_procedimentos, prontdent_orientacoes, prontdent_tuss_texto, "
                 "prontdent_cid_texto, prontdent_solicitacao_exame_texto, prontdent_responsavel, prontdent_data_finalizado, prontdent_tempo_duracao, "
                 "prontdent_codigo_medico, prontdent_consulta",
                 [pid, ("Retorno: " + caso["queixa"].lower()) if retorno else caso["queixa"],
                  ("Retorno para continuidade do tratamento. " + caso["anamnese"]) if retorno else caso["anamnese"],
                  "Paciente colaborativo(a).", rng.choice(["Boa", "Regular", "Deficiente"]), rng.choice(["Normal", "Inflamada", "Sangramento à sondagem"]),
                  rng.choice(["Classe I de Angle", "Classe II de Angle", "Mordida cruzada posterior"]), "Sem ruídos ou dor à palpação",
                  "Sem alterações", "Normocoradas", "Sem alterações", "Sem alterações", "Sem alterações", "Sem alterações",
                  rng.choice(["Rosa, firme", "Edemaciada na região posterior"]), rng.choice(["Nenhum", "Bruxismo", "Onicofagia", "Tabagismo"]),
                  rng.choice(["Não", "Não", "Sim - aparelho fixo"]), None, f"{rng.randint(105, 140)}/{rng.randint(65, 90)}", str(rng.randint(60, 95)),
                  f"{rng.uniform(1.5, 1.9):.2f}", f"{rng.uniform(36.0, 37.2):.1f}", f"{rng.uniform(50, 100):.1f}", "Ausente", "Atípica", "Não palpáveis",
                  caso["diagnostico"], caso["plano"], "Analgesia" if not retorno else None, None,
                  "Ibuprofeno 400 mg - 1 comprimido a cada 8 horas por 3 dias, se dor." if not retorno else None,
                  "Radiografia periapical" if not retorno else None, None, caso["procedimentos"],
                  "Escovação 3x ao dia, uso de fio dental e retorno conforme planejamento.", caso["tuss"], caso["cid"],
                  "Radiografia panorâmica" if not retorno else None, pac["responsavel"], Sql(dia(idx)), tempo, prof["id"], consulta_id])
        for fdi, status, obs in caso["dentes"]:
            self.add("prontuario_dentista_dente", "prontdentd_codigo, prontdentd_prontuario, prontdentd_numero_fdi, prontdentd_status, prontdentd_observacao",
                     [self.novo_id("prontuario_dentista_dente"), pid, fdi, status, obs])
        return ("dentista", pid)

    def planejamento(self, org_id, prof, pac, consulta_id, idx, fim_min, prontuario):
        """Itens do planejamento terapêutico, com token/expiração coerentes com o changeset 060
        (link de 7 dias, expiração só vale para itens PENDENTES)."""
        rng = self.rng
        criado = ts(idx, fim_min, 5)
        sorteio = rng.random()
        if idx <= -5:
            estado = "ASSINADO" if sorteio < 0.95 else "EXPIRADO"
        else:
            estado = "PENDENTE_LINK" if sorteio < 0.7 else "PENDENTE_SEM_LINK"
        token = self.uuid() if estado != "PENDENTE_SEM_LINK" else None
        expiracao = Sql(f"({criado} + INTERVAL 7 DAY)") if token else None
        for proc, valor in pac["caso"]["procs"]:
            assinado = estado == "ASSINADO"
            self.add("tb_planejamento_terapeutico", "id, prontuario_dentista_id, consulta_id, paciente_id, profissional_id, organizacao_id, data_procedimento, "
                     "procedimento_realizado, valor, token_assinatura, assinatura_base64, data_assinatura, status_assinatura, created_at, updated_at, "
                     "ip_origem, prontuario_id, token_expiracao",
                     [self.novo_id("tb_planejamento_terapeutico"), prontuario[1] if prontuario[0] == "dentista" else None, consulta_id, pac["id"],
                      prof["id"], org_id, Sql(dia(idx)), proc, valor, token, ASSINATURA_PNG if assinado else None,
                      Sql(f"({criado} + INTERVAL 2 HOUR)") if assinado else None, "ASSINADO" if assinado else "PENDENTE",
                      Sql(criado), Sql(f"({criado} + INTERVAL 2 HOUR)") if assinado else Sql(criado),
                      f"189.{rng.randint(1, 254)}.{rng.randint(1, 254)}.{rng.randint(1, 254)}" if assinado else None,
                      prontuario[1] if prontuario[0] == "medico" else None, expiracao])

    def respostas_questionario(self):
        respostas = []
        for i, pergunta in enumerate(PERGUNTAS_QUESTIONARIO):
            sim = i in OBS_SIM and self.rng.random() < 0.3
            respostas.append(dict(pergunta=pergunta, resposta="Sim" if sim else "Não", observacao=OBS_SIM.get(i, "") if sim else ""))
        return json.dumps(respostas, ensure_ascii=False)

    def termo_assinado(self, org_id, pac, consulta_id, idx, minuto):
        criado = f"({ts(idx, minuto)} - INTERVAL 1 DAY)"
        self.add("tb_termo_autorizacao", "id, consulta_id, paciente_id, organizacao_id, token, data_expiracao, data_assinatura, assinatura_base64, "
                 "ip_origem, respostas_questionario, status, created_at, updated_at",
                 [self.novo_id("tb_termo_autorizacao"), consulta_id, pac["id"], org_id, self.uuid(), Sql(f"({criado} + INTERVAL 24 HOUR)"),
                  Sql(f"({criado} + INTERVAL 3 HOUR)"), ASSINATURA_PNG, f"177.{self.rng.randint(1, 254)}.{self.rng.randint(1, 254)}.{self.rng.randint(1, 254)}",
                  self.respostas_questionario(), "ASSINADO", Sql(criado), Sql(f"({criado} + INTERVAL 3 HOUR)")])
        pac["tem_termo"] = True

    def termo_pendente(self, org_id, pac, consulta_id):
        horas = self.rng.randint(1, 12)
        self.add("tb_termo_autorizacao", "id, consulta_id, paciente_id, organizacao_id, token, data_expiracao, data_assinatura, assinatura_base64, "
                 "ip_origem, respostas_questionario, status, created_at, updated_at",
                 [self.novo_id("tb_termo_autorizacao"), consulta_id, pac["id"], org_id, self.uuid(), Sql(f"(@agora + INTERVAL {24 - horas} HOUR)"),
                  None, None, None, None, "PENDENTE", Sql(f"(@agora - INTERVAL {horas} HOUR)"), Sql(f"(@agora - INTERVAL {horas} HOUR)")])
        pac["tem_termo"] = True

    def gerar_chamados(self, t, org_id, gestores):
        modelos = [
            ("Como exportar o histórico do paciente em PDF?", "Gostaria de saber se é possível exportar o histórico completo do paciente para enviar ao convênio.",
             "DUVIDA_TECNICA", "BAIXA", "CONCLUIDO", 20),
            ("Sugestão: lembrete por WhatsApp", "Seria ótimo enviar o lembrete de consulta também por WhatsApp, além do e-mail.",
             "SUGESTAO_MELHORIA", "MEDIA", "EM_ANALISE", 3),
            ("Agenda demorando para carregar", "Na visão semanal com todos os profissionais a agenda leva alguns segundos para abrir.",
             "PROBLEMA_SISTEMA", "ALTA", "EM_ANDAMENTO", 6),
        ]
        for titulo, corpo, categoria, prioridade, status, dias in self.rng.sample(modelos, 2):
            gestor = self.rng.choice(gestores)
            chamado_id = f"{self.rng.randint(0, 999999):06d}-{self.rng.randint(0, 999999):06d}-{self.rng.randint(0, 99999):05d}"
            prazo = {"BAIXA": 5, "MEDIA": 3, "ALTA": 1}[prioridade]
            criado = f"(@agora - INTERVAL {dias} DAY)"
            self.add("chamado_suporte", "id, organizacao_id, usuario_criador_id, criador_nome, criador_email, titulo, corpo, categoria, prioridade, status, "
                     "previsao_atendimento, data_criacao, data_atualizacao",
                     [chamado_id, org_id, gestor["uid"], gestor["nome"], gestor["email"], titulo, corpo, categoria, prioridade, status,
                      Sql(f"DATE(({criado}) + INTERVAL {prazo} DAY)"), Sql(criado), Sql(f"({criado} + INTERVAL 1 DAY)")])
            self.mensagem(org_id, gestor["id"], gestor["email"], gestor["nome"], f"Chamado #SC-{chamado_id} recebido",
                          f"Recebemos o seu chamado \"{titulo}\". Nossa equipe retornará em até {prazo} dia(s) útil(eis).",
                          "EMAIL_CHAMADO_SUPORTE", f"({criado} + INTERVAL 1 MINUTE)")

    # ----------------------------------------------------------------- saída
    def escrever(self, caminho):
        linhas = [CABECALHO.format(seed=self.seed), PREPARO_DATAS(), LIMPEZA, REFERENCIAS, "START TRANSACTION;", ""]
        ordem = ["endereco", "organizacao", "usuarios", "admin_organizacao", "secretaria", "profissional", "profissional_especialidade",
                 "tb_procedimento_padrao", "assinatura_tenant", "configuracoes_consulta", "paciente", "consulta", "consulta_historico",
                 "prontuario", "prontuario_dentista", "prontuario_dentista_dente", "tb_planejamento_terapeutico", "tb_termo_autorizacao",
                 "mensageria", "chamado_suporte"]
        assert set(ordem) == set(self.rows), set(self.rows) ^ set(ordem)
        for tabela in ordem:
            colunas, valores = self.rows[tabela]
            linhas.append(f"-- {tabela}: {len(valores)} linha(s)")
            for i in range(0, len(valores), 200):
                linhas.append(f"INSERT INTO `{tabela}` ({colunas}) VALUES\n" + ",\n".join(valores[i:i + 200]) + ";")
            linhas.append("")
            if tabela == "assinatura_tenant":
                linhas.append(self.sql_cobrancas())
        linhas.append("COMMIT;")
        linhas.append(RODAPE)
        Path(caminho).write_text("\n".join(linhas), encoding="utf-8")

    def sql_cobrancas(self):
        """Uma cobrança PAGA por ciclo mensal desde data_inicio até hoje (quantidade depende da data de importação)."""
        partes = ["-- cobranca_tenant: mensalidades PAGAS (forma MANUAL, sem IDs do Mercado Pago -> webhook/reconciliação não as tocam)"]
        for aid, org_id, valor, ini in self.cobrancas:
            partes.append(
                "INSERT INTO `cobranca_tenant` (assinatura_tenant_id, organizacao_id, valor_total, status, txid, data_pagamento, criada_em, "
                "mp_payment_id, mp_authorized_payment_id, forma_pagamento, motivo_recusa)\n"
                f"SELECT {aid}, {org_id}, {valor:.2f}, 'PAGO', NULL, TIMESTAMP(DATE_ADD({ini}, INTERVAL k MONTH), '10:15:00'), "
                f"TIMESTAMP(DATE_ADD({ini}, INTERVAL k MONTH), '08:00:00'), NULL, NULL, 'MANUAL', NULL\n"
                "FROM (SELECT 0 AS k UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 "
                "UNION ALL SELECT 6 UNION ALL SELECT 7) meses\n"
                f"WHERE DATE_ADD({ini}, INTERVAL k MONTH) <= @hoje ORDER BY k;")
        return "\n".join(partes) + "\n"


CABECALHO = """-- =====================================================================================
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!  ATENÇÃO  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
--  ESTE SCRIPT APAGA TODOS OS DADOS DE NEGÓCIO DO BANCO EM QUE FOR EXECUTADO
--  (usuários, clínicas, pacientes, consultas, prontuários, assinaturas, cobranças...)
--  e recria uma massa de DEMONSTRAÇÃO. NUNCA rode em produção.
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- =====================================================================================
-- SaúdeConecta - seed de demonstração (MySQL 8.0.x). Gerado por docs/demo/gerar_seed_demo.py (seed={seed}).
-- Rodar: mysql -u root -p office < /caminho/para/saudeConectaAPI/docs/demo/seed-demo.sql   (troque root/office; sem USE fixo: vale o banco escolhido)
-- Pré-requisito: o schema já criado pelo Liquibase da aplicação (db.changelog-master.xml até o 066).
-- Idempotente: cada execução apaga e recria tudo; as contagens ficam iguais entre execuções.
--
-- DATAS: tudo é relativo ao momento da importação, no horário de Brasília (UTC-3, sem horário de verão),
-- independente do fuso da sessão MySQL. @hoje/@agora e @d_mN/@d_pN (N-ésimo dia útil para trás/para frente;
-- @d_p0 = hoje, ou o próximo dia útil se a importação cair num fim de semana).
--
-- TABELAS APAGADAS (TRUNCATE) e repovoadas:
--   usuarios (o ROOT é recriado com o mesmo login do changeset 031), organizacao, endereco, admin_organizacao,
--   secretaria, profissional, profissional_especialidade, paciente, consulta, consulta_historico,
--   prontuario, prontuario_dentista, prontuario_dentista_dente, tb_planejamento_terapeutico,
--   tb_termo_autorizacao, tb_procedimento_padrao, configuracoes_consulta, assinatura_tenant,
--   cobranca_tenant, mensageria, chamado_suporte.
-- TABELAS APAGADAS e deixadas vazias (são geradas pelo uso ou têm padrão automático):
--   atendimento_rascunho (rascunhos de atendimento, changeset 066), chamado_suporte_anexo, historico_dados_pessoais, auditoria_acesso_master, mp_webhook_evento,
--   configuracao_card_dashboard e configuracao_grafico_dashboard (o service devolve o padrão quando vazias).
-- APAGADA PARCIALMENTE: forma_pagamento -> só as linhas de clínica (organizacao_id NOT NULL);
--   as 6 formas globais do changeset 012-004 ficam.
-- PRESERVADAS (dados de referência dos changesets, usadas por código/lookup):
--   DATABASECHANGELOG, DATABASECHANGELOGLOCK (Liquibase), tipo_profissional (012-001),
--   especialidade (012/050/055), plano_assinatura (027/053/054), forma_pagamento global (012-004).
--   As chaves dessas tabelas são buscadas por código (nunca por id fixo).
-- =====================================================================================
SET NAMES utf8mb4;
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
"""


def PREPARO_DATAS():
    linhas = ["-- ---------------------------------------------------------------- datas dinâmicas",
              "SET @agora = CAST(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '-03:00') AS DATETIME);",
              "SET @agora = @agora - INTERVAL SECOND(@agora) SECOND;",
              "SET @hoje = DATE(@agora);",
              "DROP TEMPORARY TABLE IF EXISTS demo_dias;",
              "CREATE TEMPORARY TABLE demo_dias (idx INT PRIMARY KEY, dia DATE NOT NULL);",
              "INSERT INTO demo_dias (idx, dia)",
              "WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 70)",
              "SELECT 0 - CAST(ROW_NUMBER() OVER (ORDER BY d DESC) AS SIGNED), d",
              "FROM (SELECT DATE_SUB(@hoje, INTERVAL n DAY) AS d FROM seq) x WHERE DAYOFWEEK(d) NOT IN (1, 7);",
              "INSERT INTO demo_dias (idx, dia)",
              "WITH RECURSIVE seq (n) AS (SELECT 0 UNION ALL SELECT n + 1 FROM seq WHERE n < 70)",
              "SELECT CAST(ROW_NUMBER() OVER (ORDER BY d) AS SIGNED) - 1, d",
              "FROM (SELECT DATE_ADD(@hoje, INTERVAL n DAY) AS d FROM seq) x WHERE DAYOFWEEK(d) NOT IN (1, 7);"]
    for i in range(-DIAS_UTEIS, DIAS_UTEIS + 1):
        linhas.append(f"SET {dia(i)} = (SELECT dia FROM demo_dias WHERE idx = {i});")
    for t in TENANTS:
        linhas.append(f"SET @ini_{t['chave']} = DATE_SUB(@hoje, INTERVAL {t['inicio_dias']} DAY);  -- início da assinatura de {t['nome']}")
    linhas.append("DROP TEMPORARY TABLE IF EXISTS demo_dias;")
    return "\n".join(linhas) + "\n"


LIMPEZA = """-- ---------------------------------------------------------------- limpeza (FK desligada só aqui)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `atendimento_rascunho`;
TRUNCATE TABLE `chamado_suporte_anexo`;
TRUNCATE TABLE `chamado_suporte`;
TRUNCATE TABLE `mensageria`;
TRUNCATE TABLE `historico_dados_pessoais`;
TRUNCATE TABLE `auditoria_acesso_master`;
TRUNCATE TABLE `mp_webhook_evento`;
TRUNCATE TABLE `cobranca_tenant`;
TRUNCATE TABLE `assinatura_tenant`;
TRUNCATE TABLE `tb_termo_autorizacao`;
TRUNCATE TABLE `tb_planejamento_terapeutico`;
TRUNCATE TABLE `tb_procedimento_padrao`;
TRUNCATE TABLE `prontuario_dentista_dente`;
TRUNCATE TABLE `prontuario_dentista`;
TRUNCATE TABLE `prontuario`;
TRUNCATE TABLE `consulta_historico`;
TRUNCATE TABLE `consulta`;
TRUNCATE TABLE `configuracoes_consulta`;
TRUNCATE TABLE `configuracao_card_dashboard`;
TRUNCATE TABLE `configuracao_grafico_dashboard`;
TRUNCATE TABLE `profissional_especialidade`;
TRUNCATE TABLE `profissional`;
TRUNCATE TABLE `secretaria`;
TRUNCATE TABLE `admin_organizacao`;
TRUNCATE TABLE `paciente`;
DELETE FROM `forma_pagamento` WHERE organizacao_id IS NOT NULL;
TRUNCATE TABLE `usuarios`;
TRUNCATE TABLE `organizacao`;
TRUNCATE TABLE `endereco`;
-- Daqui em diante a FK fica LIGADA: qualquer inconsistência da massa aborta o script.
SET FOREIGN_KEY_CHECKS = 1;
"""

REFERENCIAS = """-- ---------------------------------------------------------------- chaves das tabelas preservadas (por código)
SET @tp_MEDICO = (SELECT id FROM tipo_profissional WHERE codigo = 'MEDICO');
SET @tp_DENTISTA = (SELECT id FROM tipo_profissional WHERE codigo = 'DENTISTA');
SET @plano_STARTER = (SELECT id FROM plano_assinatura WHERE tipo = 'STARTER');
SET @plano_PROFISSIONAL = (SELECT id FROM plano_assinatura WHERE tipo = 'PROFISSIONAL');
SET @plano_BUSINESS = (SELECT id FROM plano_assinatura WHERE tipo = 'BUSINESS');
""" + "\n".join(
    f"SET @fp_{c} = (SELECT MIN(id) FROM forma_pagamento WHERE codigo = '{c}' AND organizacao_id IS NULL);" for c, _ in FORMAS_PAGAMENTO
) + "\n" + "\n".join(
    f"SET @esp_{e} = (SELECT MIN(e.id) FROM especialidade e JOIN tipo_profissional t ON t.id = e.tipo_profissional_id "
    f"WHERE t.codigo = '{tipo}' AND e.codigo = '{e}');"
    for tipo, e in sorted({("MEDICO", m) for t in TENANTS for m in t["medicos"]} | {("DENTISTA", d) for t in TENANTS for d in t["dentistas"]})
) + "\n"

RODAPE = """SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- ---------------------------------------------------------------- resumo
SELECT 'usuarios' AS tabela, COUNT(*) AS linhas FROM usuarios
UNION ALL SELECT 'organizacao', COUNT(*) FROM organizacao
UNION ALL SELECT 'paciente', COUNT(*) FROM paciente
UNION ALL SELECT 'consulta', COUNT(*) FROM consulta
UNION ALL SELECT 'prontuario', COUNT(*) FROM prontuario
UNION ALL SELECT 'prontuario_dentista', COUNT(*) FROM prontuario_dentista
UNION ALL SELECT 'tb_planejamento_terapeutico', COUNT(*) FROM tb_planejamento_terapeutico
UNION ALL SELECT 'tb_termo_autorizacao', COUNT(*) FROM tb_termo_autorizacao
UNION ALL SELECT 'mensageria', COUNT(*) FROM mensageria
UNION ALL SELECT 'cobranca_tenant', COUNT(*) FROM cobranca_tenant;
"""


def main():
    parser = argparse.ArgumentParser(description="Gera o seed de demonstração do SaúdeConecta (MySQL).")
    parser.add_argument("--seed", type=int, default=SEED_PADRAO, help=f"semente do gerador aleatório (padrão {SEED_PADRAO})")
    parser.add_argument("--saida", default=str(Path(__file__).with_name("seed-demo.sql")), help="arquivo .sql de saída")
    args = parser.parse_args()
    gerador = Gerador(args.seed)
    gerador.gerar()
    gerador.escrever(args.saida)
    print(f"Gerado {args.saida} (seed={args.seed})")
    for tabela, (_, valores) in sorted(gerador.rows.items()):
        print(f"  {tabela:32s} {len(valores):6d}")
    print("\nLogins (login = senha):")
    for tenant, perfil, nome, login in gerador.logins:
        print(f"  {tenant:30s} {perfil:42s} {login:16s} {nome}")


if __name__ == "__main__":
    main()
