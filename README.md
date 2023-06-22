# Spring boot 3.0 - Secure your API with JWT Token [2023]

Curso tomado del canal de [**Bouali Ali**](https://www.youtube.com/watch?v=BVdQ3iuovg0)

---

## Diagrama sobre el mecanismo usado para la validación de JWT

![01.Mecanismo_de_validacion_de_jwt.png](./assets/01.Mecanismo_de_validacion_de_jwt.png)

## [12:28] Connect to the database

Establecemos las configuraciones en el **application.properties** para conectarnos a la bd de postgresql. Tener en
cuenta, que previamente creamos la base de datos **db_spring_security_jwt** usando **DBeaver**.

````properties
# Datasource
spring.datasource.url=jdbc:postgresql://localhost:5432/db_spring_security_jwt
# Ayudará a Spring para detectar o usar le mejor nombre de clase de controlador
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=magadiflo
#
# Jpa
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
# Le decimos qué base de datos estamos usando
spring.jpa.database=postgresql
# Ayudará a Spring a realizar y escribir las mejores consultas para adaptarse a nuestra base de datos postgres SQL
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
````

## [17:12] Create user class

Creamos nuestra clase User agregándole anotaciones de **lombok**, estas anotaciones nos permitirán tener el código
más limpio.

````java

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
}
````

## [20:05] Transform the User to an entity

Para convertir nuestra clase User en una entidad, debemos usar la anotación **@Entity**. Ahora, dado que estamos usando
**Spring Boot 3**, en esta versión debemos asegurarnos que el paquete a importar para dicha anotación sea de
**jakarta.persistence** y ya no de **javax.persistence**, ya que este último le corresponde a la versión de
Spring Boot 2. De igual forma, debemos importar de **jakarta.persistence** las anotaciones: **@Id, @GeneratedValue(...),
@Table(...), etc**.

A continuación se muestra nuestra clase de entidad ya completa:

````java

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
}
````

**IMPORTANTE**

En la anotación **@GenerateValue** no le estamos especificando de manera explícita la estrategia a utilizar para el
tipo de generación del valor para la clave primaria, lo que significa que por defecto usará el tipo de generación
AUTO, es decir, es como si explícitamente lo definiéramos así: ``@GeneratedValue(strategy = GenerationType.AUTO)``.

**GenerationType.AUTO:** Indica que el proveedor de persistencia debe elegir una estrategia adecuada para la base de
datos en particular. En nuestro caso, como estamos usando **Postgresql** y teniendo la anotación **@GenerateValue**
(que por defecto es AUTO si no le especificamos otro valor) en la clave primaria, al ejecutar el proyecto **nos creará
la tabla users y una secuencia** pudiéndolos observar en la consola de esta manera:

````roomsql
create sequence users_seq start with 1 increment by 50

create table users (
    id integer not null,
    email varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    password varchar(255),
    primary key (id)
)
````

Ahora, existe la posibilidad de definir el **tipo de generación en IDENTITY** para nuestra tabla que está en una base de
datos de postresql. Nuestro atributo id quedaría anotado de la siguiente manera:

````
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Integer id;
````

Al ejecutar el proyecto, nos mostrará en cosola:

````roomsql
create table users (
    id serial not null,
    email varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    password varchar(255),
    primary key (id)
)
````

Debemos observar que existe una ligera diferencia. El tipo de dato del atributo id es **serial**, mientras que en el
anterior es **integer**. PostgreSQL tiene los tipos de datos smallserial, serial y bigserial; estos no son tipos
verdaderos, sino simplemente una conveniencia notacional para crear columnas de identificador único. **Estos son
similares a AUTO_INCREMENT** propiedad admitida por otras bases de datos.

Si desea que una columna **serie** tenga una restricción única o sea una clave principal, ahora debe especificarse, al
igual que cualquier otro tipo de datos.

Otra diferencia es que en el primer caso (GenerationType.AUTO) se muestra en consola la creación de una secuencia
llamada **users_seq** que inicia en **1 y se incrementa de 50 en 50**, mientras que en el segundo caso
(GenerationType.IDENTITY), si bien es cierto no se muestra en consola ninguna creación de secuencia, al revisar la
base de datos de postgressql vemos que sí crea una secuencia llamada **users_id_seq** que **inicia en 1 y se incrementa
de 1 en 1**.

Para este proyecto, usaremos la primera opción:

````
@Id
@GeneratedValue
private Integer id;
````
