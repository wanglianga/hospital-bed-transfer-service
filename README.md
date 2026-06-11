# 医院床位转科预约服务

## 原始需求

> 医院需要床位转科预约服务，Spring Boot 接口管理床位余量、转科申请、优先级评估、占床、入科确认和取消释放。业务对象包括患者住院号、诊断、当前科室、目标科室、护理等级、隔离要求、床位类型、预约时间、护士站确认和家属通知。医生发起转科申请后，住院部根据床位、病情和隔离规则安排目标床位；护士站确认接收时间；患者未按时转入时床位释放。服务要区分无床位、需隔离、重症优先、家属拒绝、患者状态变化和占床超时。

## 项目简介

基于 Spring Boot 3.4 的医院床位转科预约服务，提供床位余量管理、转科申请与审批、优先级评估、占床管理、入科确认和超时释放等完整业务流程。

## 技术栈

- Java 17
- Spring Boot 3.4.5
- Spring Data JPA
- H2 内存数据库
- Maven

## API 接口

### 床位管理 `/api/beds`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/beds` | 创建床位 |
| PUT | `/api/beds/{id}` | 更新床位信息 |
| GET | `/api/beds` | 查询所有床位 |
| GET | `/api/beds/{id}` | 查询指定床位 |
| GET | `/api/beds/available` | 查询可用床位（支持 department、bedType、isolationType 筛选） |
| GET | `/api/beds/count/available` | 统计可用床位数 |

### 转科申请 `/api/transfers`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/transfers/apply` | 提交转科申请 |
| GET | `/api/transfers/{id}` | 查询转科申请详情 |
| GET | `/api/transfers/patient/{admissionNumber}` | 按患者住院号查询 |
| GET | `/api/transfers/status/{status}` | 按状态查询 |
| GET | `/api/transfers/pending` | 查询待处理转科（按优先级排序） |
| GET | `/api/transfers/department/{department}` | 按科室查询 |
| PUT | `/api/transfers/{id}/cancel` | 取消转科申请 |
| PUT | `/api/transfers/{id}/patient-status` | 更新患者状态 |
| POST | `/api/transfers/retry-pending` | 重试无床位的申请 |

### 占床与入科 `/api/occupation`

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/api/occupation/{transferId}/confirm` | 占床确认 |
| PUT | `/api/occupation/{transferId}/admission` | 入科确认 |
| POST | `/api/occupation/{transferId}/nurse-confirm` | 护士站确认接收 |
| POST | `/api/occupation/{transferId}/notify-family` | 通知家属 |
| PUT | `/api/occupation/{transferId}/family-response` | 家属回应（同意/拒绝） |
| PUT | `/api/occupation/{transferId}/release` | 手动释放床位 |

### 转科申请状态流转

```
PENDING → BED_ASSIGNED → OCCUPIED → ADMISSION_CONFIRMED
   ↓          ↓              ↓
NO_BED_AVAILABLE  ISOLATION_REQUIRED  TIMEOUT_RELEASED
   ↓          ↓              ↓
CRITICAL_PRIORITY  FAMILY_REFUSED    CANCELLED
                      ↓
              PATIENT_STATUS_CHANGED
```

### 优先级评估规则

评分维度：
- 患者状态：危重 +40、术后 +20、稳定 +10、出院中 +5
- 护理等级：特级 +30、一级 +25、二级 +15、三级 +5
- 隔离需求：需隔离 +15
- 床位类型：ICU/抢救 +20

优先级划分：
- ≥70 分 → EMERGENCY（紧急）
- ≥50 分 → URGENT（优先）
- ≥30 分 → NORMAL（普通）
- <30 分 → LOW（低优先）

## 启动方式

### 前置要求

- Java 17+
- Maven 3.8+（或使用项目自带 Maven Wrapper）
- Docker & Docker Compose（Docker 方式启动）

### 启动步骤

#### 方式一：Docker 一键启动（推荐）

```bash
docker compose up --build
```

后台运行：

```bash
docker compose up --build -d
```

停止服务：

```bash
docker compose down
```

访问地址：http://localhost:8080

#### 方式二：本地 Maven 启动

##### 1. 安装依赖

```bash
mvn clean install -DskipTests
```

##### 2. 启动服务

```bash
mvn spring-boot:run
```

访问地址：http://localhost:8080

H2 控制台：http://localhost:8080/h2-console（JDBC URL: `jdbc:h2:mem:hospital_transfer`，用户名: `sa`，密码为空）

## 目录结构

```
src/main/java/com/hospital/transfer/
├── HospitalTransferApplication.java   # 启动类
├── entity/                            # 实体类
│   ├── Patient.java                   # 患者信息
│   ├── Bed.java                       # 床位信息
│   ├── TransferApplication.java       # 转科申请
│   ├── NurseStationConfirmation.java  # 护士站确认
│   └── FamilyNotification.java        # 家属通知
├── enums/                             # 枚举类型
│   ├── NursingLevel.java              # 护理等级
│   ├── IsolationType.java             # 隔离类型
│   ├── BedType.java                   # 床位类型
│   ├── TransferStatus.java            # 转科状态
│   ├── PatientStatus.java             # 患者状态
│   └── PriorityLevel.java             # 优先级
├── repository/                        # 数据访问层
├── service/                           # 业务逻辑层
│   ├── BedService.java                # 床位管理
│   ├── TransferService.java           # 转科申请与优先级评估
│   └── BedOccupationService.java      # 占床、入科确认、超时释放
├── controller/                        # REST 控制器
│   ├── BedController.java
│   ├── TransferController.java
│   └── BedOccupationController.java
├── dto/                               # 数据传输对象
└── exception/                         # 异常处理
```
