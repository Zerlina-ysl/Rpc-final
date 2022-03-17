drop table tb_user if exists;

create table tb_user(
id integer not null auto_increment,
name varchar (32),
age int(3),
gender varchar (8),
primary key(id)
);

insert into tb_user(id,name,age,gender)
values
    (default ,'aa',20,'男'),
    (default ,'bb',24,'男'),
    (default ,'acc',22,'男'),
    (default ,'da',24,'男'),
    (default ,'e',29,'男'),
    (default ,'ca',20,'男'),
    (default ,'gg',27,'男'),
    (default ,'as',78,'男');
