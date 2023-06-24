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

````
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

## [25:22] Extend the User to UserDeatils object

Recordemos, que según el libro de
[**Spring Security In Action 2020**](https://github.com/magadiflo/spring-security-in-action-2020.git) que estudiamos
previo al desarrollo de este tutorial, nos enseñaba que el **UserDetails es el usuario que Spring Security reconoce
como tal dentro de su arquitectura**, es decir, el usuario con el que interactuará en diversos procesos, como la
autenticación, autorización, etc. Además, en el libro nos enseñaba a separar las responsabilidades, es decir, tener
una **Entity User**, para poder almacenar sus datos en la base de datos, eso incluiría atributos dependiendo de la
lógica de negocio, así como los clásicos atributos de todo User (username, password, roles, etc.) y una clase
**SecurityUser**, correspondiente a un **usuario que spring security reconoce dentro de su arquitectura**, es decir, una
clase que implemente el UserDetails. Luego, esta clase SecurityUser recibía por constructor la entity User, para que
internamente los métodos sobreescritos del UserDetails hagan uso de los atributos de la entity User. De esta forma,
separamos las responsabilidades: un **usuario como Entity** de un **usuario reconocido dentro de la arquitectura de
Spring Security**.

Luego de haber recordado cómo se trabajó en el libro de Spring Security In Action 2020, regresemos a este tutorial.
En este tutorial, no se separan las responsabilidades, es decir, se usa la misma clase de entidad User y se implementa
la interfaz UserDetails para convertirlo en un usuario reconocido dentro de la arquitectura de Spring Security. Esta
sería otra forma de trabajar, aunque en lo personal, me gusta más la idea de separar las responsabilidades, pero
en este caso, seguiremos tal como se está desarrollando en el tutorial.

Otro punto a observar es que anteriormente no definimos ningún rol o autoridad para la Entity User, es importante tener
este campo, ya que en la arquitectura de Spring Security, se busca que el usuario propio de esta arquitectura
devuelva en su método **getAuthorities()** una lista de roles o authorities asociados. En nuestro caso, cada usuario
tendrá un único rol: USER o ADMIN. Para eso crearemos un enum Role:

````java
public enum Role {
    USER, ADMIN
}
````

**NOTA**

> En el libro **Spring Security In Action 2020** también se aborda la diferencia entre **Rol y Authority**. Para
> resumir, un Rol es más amplio, es decir, un Rol contiene un conjunto de Authorities. **A los Authorities también se
> les conoce como permisos**. Ahora, el método **getAuthorities()** que sobreescribimos de la interfaz **UserDetails**,
> no hará una distinción y contendrá, si se da el caso, tanto los roles y authorities. Entonces, **¿dónde se hará la
> diferencia?**, esta diferencia la observaremos cuando aseguremos los endpoints ya sea usando **hasRole(),
> hasAnyRole()** o usando **hasAuthority(), hasAnyAuthority()**.
>
> Ejm que podríamos usar:<br>
> **ROLE:** ADMIN, USER, MANAGER, STUDENT<br>
> **AUTHORITY:** user:read, admin:read, admin:write

Finalmente, nuestra Entity User que ahora implementa la interfaz UserDeatils, no solamente será nuestra Entity User,
sino que ahora también en un usuario que será reconocido dentro de la arquitectura de Spring Security:

````java

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
````

## [33:32] Create the user repository

Creamos el repositorio **UserRepository** para realizar las operaciones con nuestra entity User y la base de datos,
además definimos un método personalizado para poder recuperar un entity User de la base de datos a partir de un email:

````java
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
}
````

## [35:50] Create the JWT authentication filter

Recordemos cuáles son los componentes principales en el proceso de autenticación que vimos en el libro de
**Spring Security In Action**:

![02.Main-components-authentication-spring-security.png](./assets/02.Main-components-authentication-spring-security.png)

Fijémonos en el primer componente, el **Authentication Filter**, este componente se encargará de interceptar todas las
solicitudes provenientes del cliente.

En este apartado **crearemos un Authentication Filter** personalizado al que le llamaremos **JwtAuthenticationFilter**
y será nuestro Filtro de Autenticación que configuraremos más adelante para que sea nuestro primer componente en el
proceso de autenticación de Spring Security.

Nuestra estructura inicial del **JwtAuthenticationFilter** quedaría así:

````java

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

    }
}
````

Cuando extendemos la clase abstracta **OncePerRequestFilter**, implementamos su método abstracto **doFilterInternal**
y como estamos trabajando con IntelliJ IDEA, este nos lanza un warning sugiriéndonos que los parámetros del método
implementado deberían llevar la anotación **@NonNull**, así que le agregamos dicha anotación a cada parámetro.

**@NonNull**, una anotación común de Spring para declarar que los elementos anotados no pueden ser nulos. Debe usarse en
el nivel de parámetro, valor devuelto y campo.

## [40:58] Checking the JWT Token

Siguiendo el diagrama que mostramos al inicio de este archivo (diagrama sobre el mecanismo usado para la validación
de JWT) observamos que lo primero que hacemos dentro del **JWTAuthenticationFilter** es verificar si tenemos el token
correcto, así que en este apartado implementaremos dicha funcionalidad, **Check JWT token**:

````java

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOG.error("No procesó la solicitud de autenticación porque no pudo encontrar el encabezado " +
                    "Authorization o el valor del encabezado Authorization no inicia con Bearer .");
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authHeader.substring(7);
        LOG.info("jwt obtenido: {}", jwt);
    }
}
````

## [44:32] Create the JWT service

Después de verificar el JWT token, debemos llamar al UserDetailsService para verificar si ya tenemos al usuario dentro
de nuestra base de datos o no. Ahora, para hacer eso debemos llamar a un servicio JWT para extraer el nombre de usuario.

Para este capítulo solo creamos la clase de servicio que usaremos en el JwtAuthenticationFilter:

````java

@Service
public class JwtService {
    public String extractUsername(String token) {
        return null;
    }
}
````

Inyectamos nuestro JwtService en nuestro JwtAuthenticationFilter:

````java

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /* omitted code */
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        /* omitted code */
        final String userEmail;
        /* omitted code */
        jwt = authHeader.substring(7);
        userEmail = this.jwtService.extractUsername(jwt);
        LOG.info("jwt obtenido: {}", jwt);
    }
}
````

## [47:56] Add the JJWT Dependencies

Para poder manipular el JWT Token, ya sea extrayendo información de él, o validándolo, etc... necesitamos incluir
nuevas dependencias dentro del pom.xml

````xml

<dependencies>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
    </dependency>
</dependencies>
````

## [53:06] Extract claims from JWT

Implementamos un método que nos retorne los claims, para eso es necesario hacer uso de nuestra clave con el que firmamos
el token, pero eso lo haremos en el siguiente capítulo, por ahora solo retornamos los claims a partir de un token:

````java

@Service
public class JwtService {
    /* omitted code */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(this.getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    /* omitted code */
}
````

## [55:23] Implement the getSignInKey method

Temporalmente, dejaremos la clave en este archivo, posteriormente lo agregaremos en el application.properties. Ahora, lo
que hacemos en este capítulo es definir la clave que usaremos para poder firmar los tokens que vayamos a crear o
poder comprobar que un token retornado tenga la misma firma con el que se generó:

````java

@Service
public class JwtService {
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    /* omitted code */

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
````

## [01:00:07] Extract a single claim from JWT

Para poder extraer un claim de JWT usamos una función genérica que entre sus parámetros recibe un tipo **T** y además
debe retornar un valor del mismo tipo **T**:

````java

@Service
public class JwtService {
    /* omitted code */

    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = this.extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /* omitted code */
}
````

## [01:01:51] Extract the username from the token

En el capítulo anterior, solo mostramos la función genérica que creamos, pero no explicamos a profundidad, y pienso que
es importante hacerlo, ya que recuperamos el username a partir de dicha función genérica. Entonces, es necesario conocer
exactamente cómo funciona.

En primer lugar, veamos que un **Claims** puede retornar distintos tipos de datos (string, date, etc). A continuación se
muestra un claims obtenido a partir de un token:

````
Claims claims = this.extractAllClaims(token);

String subject  = claims.getSubject();
Date expiration = claims.getExpiration();
Date issuedAt   = claims.getIssuedAt();
````

En este capítulo, lo que queremos hacer es retornar un **username o getSubject()** a partir de un claims que ha sido
construido usando un token. Ahora, qué pasa si más adelante quisiéramos retornar la **fecha de expiración (expiration)**
y más adelante quizá **la fecha de emisión (issuedAt)**, etc. entonces, para usar un único método que nos retorne el
tipo de dato deseado del claims es que construimos en el capítulo anterior el método genérico.

**Explicando el método genérico del capítulo anterior**

Decimos que **es un método genérico porque lleva este símbolo** ```<T>```, lo que significa que en algún parámetro del
método debe estar definido ese tipo de dato ``T``, y si observamos el método genérico el segundo parámetro que es una
**Function** tiene definido el tipo de dato ``T``. Además, se definió que el tipo de dato que nos debe retornar la
función genérica tiene que ser del tipo ``T``.

````
public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
}
````

Nuestra función genérica recibe como primer parámetro un **token** de tipo String, este token se usará internamente para
obtener un objeto del tipo **Claims**.

````
final Claims claims = this.extractAllClaims(token);
````

Ahora, lo que nos retornará nuestra función genérica, será la ejecución de la interfaz funcional **Function** que como
parámetro de entrada recibe un **claims**, el mismo que se obtuvo en el paso anterior.

````
return claimsResolver.apply(claims);
````

Pero, **¿cómo definimos el tipo de dato de retorno de la función genérica?** El tipo de dato de retorno será definido al
momento de usar nuestra función genérica, por ejemplo: si quiero obtener el username haríamos lo siguiente:

````
String username = this.extractClaims(token, claims -> claims.getSubject());
````

De esta manera vemos que el tipo de dato de retorno del método genérico es de tipo **String**, porque la implementación
de la interfaz funcional **Function** eso es lo que nos está retornando ``claims -> claims.getSubject()``.
Nótese que el **claims** que usamos en la implementación corresponde al **claims** agregado por parámetro en el método
**apply(claims)**.

Un ejemplo más, supongamos que ahora necesitamos recuperar la fecha de expiración, entonces usamos nuestro método
genérico de la siguiente manera:

````
Date date = this.extractClaims(token, claims -> claims.getExpiration());
````

De esta manera vemos que ahora el tipo de dato de retorno del método genérico es de tipo **Date**.

## [01:02:52] Generate the JWT token

Para generar el jwt usamos la librería que agregamos a las dependencias del pom.xml. El primer método para generar el
token recibe un mapa con información adicional que quisiéramos que nuestro token tenga aparte de los que normalmente
extrae del userDetails:

````java

@Service
public class JwtService {
    /* omitted code */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 24))) // Expira en 24 horas (en milisegundos)
                .signWith(this.getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    /* omitted code */
}
````

Qué pasa si ¿no queremos agregar ninguna información adicional al token?, bueno creamos un método que use únicamente
el userDetails para construir el token, para eso solo reutilizamos el método creado anteriormente:

````java

@Service
public class JwtService {
    /* omitted code */
    public String generateToken(UserDetails userDetails) {
        return this.generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // omitted code
    }
    /* omitted code */
}
````

## [01:08:15] Check if the token is valid

Verificamos si el token es válido, para eso validamos si el nombre de usuario dentro del token es igual al usuario
contenido en el UserDetails y además si el token no ha expirado:

````java

@Service
public class JwtService {
    /* omitted code */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = this.extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !this.isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return this.extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return this.extractClaims(token, Claims::getExpiration);
    }
    /* omitted code */
}
````

## [01:11:22] Check the User existence in the database (JwtAuthFilter)

Regresamos a nuestro JwtAuthenticationFilter y continuamos en el código donde nos quedamos, esta vez **verificando que
el userEmail no sea nulo y que el usuario no esté autenticado** para proceder a buscar el usuario en la base de datos.
Para eso necesitamos usar la interfaz UserDetailsService y llamar a su método **loadUserByUsername(userEmail);**, en
el siguiente capítulo realizamos nuestra propia implementación de dicha interfaz, por ahora quedaría así el código:

````java

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    /* omitted code */
    private final UserDetailsService userDetailsService;

    /* omitted code */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        /* omitted code */
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
        }
    }
}
````

## [01:15:13] Implement the UserDetailsService

En este punto, necesitamos crear un **bean** del tipo **UserDetailsService** o crear una clase que implemente el
**UserDetailsService** anotándolo con @Service o con @Component para que se convierta en un **bean administrado por
Spring**:

````java

@RequiredArgsConstructor
@Configuration
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> this.userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username no encontrado: " + username));
    }
}
````

## [01:19:38] Update the SecurityContextHolder and finalice the filter

Continuamos con la implementación del JwtAuthenticationFilter. Usamos los métodos creados en nuestra clase JwtService
para verificar si el token es válido. **Si el usuario es válido, entonces debemos actualizar el SecurityContext** y
enviar la solicitud a nuestro dispatcherServlet.

**Para actualizar el SecurityContext**, necesitamos crear un objeto que implemente la interfaz **Authentication**.
Spring cuenta con una clase que implementa el **Authentication** y será el que usaremos para crear el objeto de
autenticación, esta clase es **UsernamePasswordAuthenticationToken**, luego usaremos el objeto creado para actualizar
el SecurityContext:

````java

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /* omitted code */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        /* omitted code */

        // Verificamos que el userEmail no sea nulo y que el usuario no esté autenticado
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            if (this.jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                // Ampliamos o reforzamos este UsernamePasswordAuthenticationToken con los detalles de nuestra solicitud
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //Actualizamos el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
````

Finalmente, la clase completa de nuestro JwtAuthenticationFilter quedaría así:

````java

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOG.error("No procesó la solicitud de autenticación porque no pudo encontrar el encabezado " +
                    "Authorization o el valor del encabezado Authorization no inicia con Bearer .");
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authHeader.substring(7);
        userEmail = this.jwtService.extractUsername(jwt);
        LOG.info("username: {}, jwt: {}", userEmail, jwt);

        // Verificamos que el userEmail no sea nulo y que el usuario no esté autenticado
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            if (this.jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                // Ampliamos o reforzamos este UsernamePasswordAuthenticationToken con los detalles de nuestra solicitud
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //Actualizamos el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
````

## [01:23:53] Add the security configuration

Crearemos una clase que será nuestra **clase principal de configuración de Spring Security**, esta clase tendrá las
anotaciones **@Configuration y @EnableWebSecurity** siempre juntas. La anotación **@Configuration** como sabemos es un
estereotipo de @Component y sirve para definir que la clase será una clase de configuración. La anotación
**@EnableWebSecurity**, nos permite crear una clase de seguridad personalizada. Esta anotación se coloca en una clase de
configuración de Spring y le indica al framework que configure automáticamente la seguridad web en el proyecto. Al
colocar @EnableWebSecurity en una clase de configuración, Spring Security activa la integración con Spring MVC y
establece una serie de configuraciones por defecto para proteger los endpoints de la aplicación web.

El SecurityFilterChain es el bean responsable de configurar toda la seguridad HTTP de nuestra aplicación.

**NOTA**
> Como ahora estoy usando Spring Security 6.1.1 dentro de Spring Boot 3.1.1, la configuración tiende a ser algo
> distinta al del video, pese a que también está usando Spring Boot 3.0.0

Nuestra clase de configuración quedaría de la siguiente manera:

````java

@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("").permitAll()
                            .anyRequest().authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(this.authenticationProvider)
                .addFilterBefore(this.jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
````

## [01:32:51] Create the authentication provider bean

En el código anterior definimos dos propiedades finales que serán inyectadas mediante **inyección de dependencias**.
Por un lado tenemos la clase **JwtAuthenticationFilter** que ya construimos en capítulos anteriores, y, por otro lado,
nos falta implementar un **AuthenticationProvider**. Entonces, para poder implementar este **AuthenticationProvider**
nos vamos a la clase de configuración **ApplicationConfig** y definimos su implementación a través de la exposición
de un **@Bean**.

Spring Security ofrece varias implementación de un AuthenticationProvider, nosotros usaremos una de esas
implementaciones, el **DaoAuthenticationProvider**.

Recordar que cuando vimos en el libro de **Spring Security In Action 2020**, en los componentes principales del proceso
de autenticación, uno de esos componentes era el **Authentication Provider**, el cual definía la lógica de
autenticación. Este Authentication Provider para encontrar al usuario usa el **UserDetailsService** y para verificar
la contraseña usa el **passwordEncoder**.

De lo dicho anteriormente, es que realizaremos la implementación del **AuthenticationProvider** configurándole los
dos elementos descritos: **UserDetailsService y el PasswordEncoder**:

````java

@RequiredArgsConstructor
@Configuration
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> this.userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username no encontrado: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(this.userDetailsService());
        authenticationProvider.setPasswordEncoder(this.passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
````

## [01:36:41] Create the authentication manager bean

Se necesita un paso más para poder finalizar la clase de configuración del capítulo anterior, que es definir el
**Authentication Manager**. El authentication manager es el responsable de administrar la autenticación.

Como recordaremos del libro de **Spring Security In Action 2020** uno de los elementos en el proceso de autenticación
es el **Authentication Manager** cuya responsabilidad es administrar el proceso de autenticación, y para esto el
Authentication Manager usa el **Authentication Provider**, por lo que anteriormente ya configuramos un
Authentication Provider, y ahora solo basta configurar el AuthenticationManager.

Para definir un **Authentication Manager** solo usamos el que Spring Boot proporciona por defecto, para eso hacemos una
inyección de dependencia del **AuthenticationConfiguration** a través del parámetro del método y lo usamos para poder
retornar la información sobre el **AuthenticationManager**.

````java

@RequiredArgsConstructor
@Configuration
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> this.userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username no encontrado: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(this.userDetailsService());
        authenticationProvider.setPasswordEncoder(this.passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
````

## [01:38:14] Create the authentication controller

Crearemos un controlador con dos métodos handler, uno para **registrarnos y otro para hacer login**. Por ahora crearemos
solo la estructura de nuestro controlador, posteriormente lo implementaremos:

````java

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/auth")
public class AuthenticationController {
    @PostMapping(path = "/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        //TODO implementar registro
        return null;
    }

    @PostMapping(path = "/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        //TODO implementar login
        return null;
    }
}
````

## [01:40:55] Create the authentication response class

Crearemos la clase que será la respuesta que enviaremos al cliente:

````java

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String token;
}
````

## [01:41:47] Create the register request object

Creamos la clase **RegisterRequest** que contendrán los datos que el cliente envíe a nuestro endpoint:

````java

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
}
````

## [01:42:50] Create the authentication request class

Clase que contendrá las credenciales enviadas desde el cliente al endpoint:

````java

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
    private String email;
    private String password;
}
````

## [01:43:22] Create the authentication service

Nuestro controlador **AuthenticationController** recibirá las solicitudes para **registrarse** y hacer **login**, luego
usará una clase de servicio para poder implementar la lógica correspondiente:

````java

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping(path = "/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(this.authenticationService.register(request));
    }

    @PostMapping(path = "/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(this.authenticationService.authenticate(request));
    }
}
````

````java

@RequiredArgsConstructor
@Service
public class AuthenticationService {

    public AuthenticationResponse register(RegisterRequest request) {
        return null;
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        return null;
    }
}
````

## [01:45:37] Implement the register method

Implementamos la lógica de registro en la clase de servicio AuthenticateService:

````java

@RequiredArgsConstructor
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationResponse register(RegisterRequest request) {
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(this.passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        this.userRepository.save(user);
        String jwtToken = this.jwtService.generateToken(user);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    /* omitted code */
}
````

## [01:49:28] Implement the authenticate method

Recordemos que el **@Bean AuthenticationManager** que expusimos en el archivo de configuración **ApplicationConfig**
tiene un método llamado **authenticate(...)** que nos permite autenticar un usuario basado en el nombre de usuario
y su contraseña, por lo que ahora necesitamos hacer una inyección de dependencia del **AuthenticationManager** en
nuestra clase de servicio **AuthenticationService** para usar dicho método.

El método recibe un objeto del tipo **UsernamePasswordAuthenticationToken** pasándole por parámetro el nombre de
usuario y su contraseña

**NOTA**

> Recordar que en este proceso de login estamos usando el constructor del UsernamePasswordAuthenticationToken que recibe
> dos parámetros, eso significa que este objeto aún no está autenticado, sino que va a iniciar un proceso de
> autenticación, por lo tanto, internamente se asigna un **setAuthenticated(false)**. En consecuencia, usamos el
> constructor con dos parámetros cuando construimos inicialmente el objeto de autenticación y aún no está autenticado
>
> Ahora, recordemos que en el **JwtAuthenticationFilter** realizamos la autenticación del usuario basado en el JWT que
> nos proporciona, una vez que verificamos que el token es válido, para autenticar al usuario creamos un objeto de la
> clase **UsernamePasswordAuthenticationToken** pero con el constructor de 3 parámetros: principal, credentials y los
> authorities. Este objeto creado, a diferencia del anterior, internamente se asigna un
> **super.setAuthenticated(true)** lo que significa que el usuario será registrado como un usuario autenticado.

Posteriormente, el objeto **UsernamePasswordAuthenticationToken** se le pasa al **AuthenticationManager** quien hará
todo el trabajo por nosotros, si el usuario o contraseña no son correctas, lanzará una excepción.

````
UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
this.authenticationManager.authenticate(authenticationToken);
````

Si luego de ejecutar las dos líneas anteriores, no se produce ninguna excepción, eso significa que el usuario está
autenticado, pues se le pasó el username y el password correctos, así que ahora solo necesitamos generar el token
y enviarlo devuelta.

````
User user = this.userRepository.findByEmail(request.getEmail()).orElseThrow();
String jwtToken = this.jwtService.generateToken(user);
return AuthenticationResponse.builder().token(jwtToken).build();
````

La clase de servicio solo con el método de autenticación sería:

````java

@RequiredArgsConstructor
@Service
public class AuthenticationService {

    /* omitted code */
    private final AuthenticationManager authenticationManager;

    /* omitted code */

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        this.authenticationManager.authenticate(authenticationToken);

        // Si se llega a este punto (no lanzó excepción) significa que la autenticación fue exitosa
        User user = this.userRepository.findByEmail(request.getEmail()).orElseThrow();
        String jwtToken = this.jwtService.generateToken(user);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }
}
````

## [01:52:17] Update the security configuration whitelist

En nuestra clase de configuración de spring security definimos el acceso permitido a los endpoints de nuestro register y
authenticate:

````java

@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/api/v1/auth/**").permitAll()
                            .anyRequest().authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(this.authenticationProvider)
                .addFilterBefore(this.jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
````

## [01:53:35] Create a demo controller

Creamos un controlador para probar la seguridad que hemos creado:

````java

@RestController
@RequestMapping(path = "/api/v1/demo-controller")
public class DemoController {
    @GetMapping
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hello from secured endpoint");
    }
}
````

## [01:54:55] Test the changes

Accediendo al endpoint del controlador demo sin enviar token de autenticación:

````bash
curl -v http://localhost:8080/api/v1/demo-controller

--- Response ---
>
< HTTP/1.1 403
<
* Connection #0 to host localhost left intact
````

Autenticándonos sin estar registrados en la Base de Datos:

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{\"email\": \"pepito@gmail.com\", \"password\": \"12345\"}" http://localhost:8080/api/v1/auth/authenticate

--- Response ---
>
< HTTP/1.1 403
<
* Connection #0 to host localhost left intact
````

Registrándonos para ser un usuario del sistema:

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{\"firstName\": \"Pepito\", \"lastName\": \"Pinto\", \"email\": \"pepito@gmail.com\", \"password\": \"12345\"}" http://localhost:8080/api/v1/auth/register

--- Response ---
>
< HTTP/1.1 200
<
{ 
  "token":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwZXBpdG9AZ21haWwuY29tIiwiaWF0IjoxNjg3NjMxMjY5LCJleHAiOjE2ODc3MTc2Njl9.ERWS895TF3Ot3tuHRIfyScHNgQeges36YNIoQehw6ag"
}
````

Accediendo al controlador Demo enviándole nuestro token de autenticación:

````bash
curl -v -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwZXBpdG9AZ21haWwuY29tIiwiaWF0IjoxNjg3NjMxMjY5LCJleHAiOjE2ODc3MTc2Njl9.ERWS895TF3Ot3tuHRIfyScHNgQeges36YNIoQehw6ag" http://localhost:8080/api/v1/demo-controller

--- Response ---
>
< HTTP/1.1 200
<
Hello from secured endpoint
````

## NOTA FINAL

En este proyecto, si un usuario accede a un endpoint sin autenticarse lanza un **403 Forbidden** cuando en realidad
debería lanzar un **401 Unauthorized**, incluso si intentamos autenticarnos con credenciales incorrectas al endpoint
para hacer login lanza el **403 Forbidden** cuando también debería lanzar el **401 Unauthorized**. Recordar que el
error **403 Forbidden** deben lanzarse cuando un usuario está autenticado en la aplicación, es decir, es un usuario
reconocido dentro del sistema pero **no tiene permiso para acceder a cierto recurso**.

Tener en cuenta ese punto para cuando se trata de tomar como referencia este proyecto.
