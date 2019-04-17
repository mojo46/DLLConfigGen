CREATE TABLE public.car_brand (
	id int8 PRIMARY KEY,
	code varchar(255) PRIMARY KEY,
	"rank" varchar(255) NULL,
	description varchar(255) NULL,
	creation_date timestamp NULL,
	created_by varchar(255) NULL,
	updated_date timestamp NULL,
	updated_by varchar(255) NULL
)
WITH (
	OIDS=FALSE
) ;


CREATE TABLE public.car_country_pmsr (
	id int8 NOT NULL,
	code varchar(255) NULL,
	description varchar(255) NULL,
	class_type varchar(255) NULL,
	code_smacc varchar(255) NULL,
	cluster_level_1 varchar(255) NULL,
	cluster_level_2 varchar(255) NULL,
	cluster_level_3 varchar(255) NULL,
	cluster_level_4 varchar(255) NULL,
	cust_type varchar(255) NULL,
	channel varchar(255) NULL,
	creation_date timestamp NULL,
	created_by varchar(255) NULL,
	updated_date timestamp NULL,
	updated_by varchar(255) NULL,
	car_market_id int8 NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (car_market_id) REFERENCES car_market(id)
)
WITH (
	OIDS=FALSE
) ;