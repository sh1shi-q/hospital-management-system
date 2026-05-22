# Hospital Management System

A comprehensive web-based Hospital Management System built with Spring Boot, providing an integrated platform for managing hospital operations, patient records, staff scheduling, and medical services.

## 📋 Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Database](#database)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

## ✨ Features

### Core Functionality
- **Patient Management** - Register, update, and maintain patient records
- **Staff Management** - Manage doctors, nurses, and administrative staff
- **Appointment Scheduling** - Book and manage patient appointments
- **Medical Records** - Maintain and access patient medical history
- **Department Management** - Organize hospital departments and specializations
- **Billing & Payments** - Track patient billing and payment status
- **Security** - Role-based access control and authentication
- **Email Notifications** - Automated email alerts and reminders

### User Roles
- **Admin** - Full system access and configuration
- **Doctor** - Patient records, appointments, and medical history
- **Nurse** - Patient care coordination and record updates
- **Receptionist** - Appointment scheduling and patient check-in
- **Patient** - View personal medical records and book appointments

## 🛠️ Technology Stack

- **Backend Framework** - Spring Boot 2.7.14
- **Language** - Java 11
- **Database** - MySQL
- **Build Tool** - Maven
- **Template Engine** - Thymeleaf
- **Security** - Spring Security
- **ORM** - Spring Data JPA
- **Email** - Spring Mail
- **Additional Libraries**:
  - Lombok - Code generation and boilerplate reduction
  - Spring Actuator - Application monitoring and metrics
  - Spring Validation - Data validation framework

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 11** or higher
- **Maven 3.6+**
- **MySQL 8.0+**
- **Git**

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/sh1shi-q/hospital-management-system.git
cd hospital-management-system
```

### 2. Create MySQL Database

```bash
mysql -u root -p
CREATE DATABASE hospital_db;
EXIT;
```

### 3. Update Database Configuration

Edit `src/main/resources/application.properties` (or `application.yml`):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hospital_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### 4. Build the Application

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application will start at `http://localhost:8080`

## ⚙️ Configuration

### Application Properties

Key configuration properties in `application.properties`:

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/

# JPA Configuration
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true

# Mail Configuration (Optional)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## 📖 Usage

### Access the Application

1. Open your web browser and navigate to `http://localhost:8080`
2. Login with default credentials or register as a new patient
3. Navigate through the application based on your user role

### Default Users

*Note: Update these credentials in production*

```
Admin:
  Username: admin
  Password: admin@123

Doctor:
  Username: doctor@hospital.com
  Password: doctor@123

Patient:
  Username: patient@email.com
  Password: patient@123
```

## 📂 Project Structure

```
hospital-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/hospital/
│   │   │   ├── controller/          # REST controllers and view controllers
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Data access layer
│   │   │   ├── model/               # Entity classes
│   │   │   ├── security/            # Security configuration
│   │   │   └── util/                # Utility classes
│   │   └── resources/
│   │       ├── templates/           # Thymeleaf templates
│   │       ├── static/              # CSS, JavaScript, images
│   │       └── application.properties
│   └── test/                        # Unit and integration tests
├── pom.xml                          # Maven configuration
├── .gitignore
└── README.md
```

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `POST /api/auth/register` - Patient registration

### Patient Management
- `GET /api/patients` - Get all patients (Admin/Doctor)
- `GET /api/patients/{id}` - Get patient details
- `POST /api/patients` - Create new patient
- `PUT /api/patients/{id}` - Update patient information
- `DELETE /api/patients/{id}` - Delete patient record

### Appointments
- `GET /api/appointments` - Get all appointments
- `POST /api/appointments` - Book new appointment
- `PUT /api/appointments/{id}` - Update appointment
- `DELETE /api/appointments/{id}` - Cancel appointment

### Doctors
- `GET /api/doctors` - List all doctors
- `GET /api/doctors/{id}` - Get doctor details
- `POST /api/doctors` - Add new doctor

### Departments
- `GET /api/departments` - List departments
- `POST /api/departments` - Create department

*Note: Detailed API documentation will be available at `/api/docs` with Swagger integration (if enabled)*

## 🗄️ Database

### Main Tables
- `users` - User accounts and authentication
- `patients` - Patient information
- `doctors` - Doctor details and specializations
- `appointments` - Appointment records
- `departments` - Hospital departments
- `medical_records` - Patient medical history
- `billing` - Billing and payment information
- `roles` - User roles and permissions

The database schema is automatically generated/updated by Hibernate based on entity classes.

## 🔐 Security

### Features
- **Spring Security** - Authentication and authorization
- **Role-Based Access Control (RBAC)** - Different access levels for different user roles
- **Password Encryption** - Bcrypt password hashing
- **Session Management** - Secure session handling
- **CSRF Protection** - Cross-Site Request Forgery protection enabled
- **SQL Injection Prevention** - Parameterized queries via JPA

### Best Practices
- Change default credentials immediately in production
- Use HTTPS in production
- Implement rate limiting for API endpoints
- Regular security audits and updates

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

For issues, questions, or suggestions, please open an issue on the [GitHub Issues](https://github.com/sh1shi-q/hospital-management-system/issues) page.

---

**Last Updated:** February 2026  
**Version:** 0.0.1-SNAPSHOT
