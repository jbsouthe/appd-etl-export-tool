select table_name from all_tables where lower(table_name) like lower('AppDynamics_DefaultTable');

describe AppDynamics_DefaultTable;

show tables;

SELECT dbms_metadata.get_ddl (object_type, object_name, owner)
FROM   all_objects
WHERE  owner = 'admin'
  AND  object_name LIKE 'AppDynamics_DefaultTable%';

select sys.all_tab_columns.column_name, sys.all_tab_columns.data_type, sys.all_tab_columns.data_length, sys.all_tab_columns.nullable
from sys.all_tab_columns
         left join sys.all_ind_columns
                   on sys.all_ind_columns.index_owner = sys.all_tab_columns.owner
                       and sys.all_ind_columns.table_name = sys.all_tab_columns.table_name
                       and sys.all_ind_columns.column_name = sys.all_tab_columns.column_name
         left join sys.all_indexes
                   on sys.all_indexes.owner = sys.all_tab_columns.owner
                       and sys.all_indexes.table_name = sys.all_tab_columns.table_name
                       and sys.all_indexes.index_name = sys.all_ind_columns.index_name
                       and sys.all_indexes.index_type = 'NORMAL'
                       and sys.all_indexes.status = 'VALID'
where lower(sys.all_tab_columns.table_name) like lower('AppDynamics_DefaultTable')
order by sys.all_tab_columns.column_id


select sys.all_tab_columns.*
from sys.all_tab_columns
         left join sys.all_ind_columns
                   on sys.all_ind_columns.index_owner = sys.all_tab_columns.owner
                       and sys.all_ind_columns.table_name = sys.all_tab_columns.table_name
                       and sys.all_ind_columns.column_name = sys.all_tab_columns.column_name
         left join sys.all_indexes
                   on sys.all_indexes.owner = sys.all_tab_columns.owner
                       and sys.all_indexes.table_name = sys.all_tab_columns.table_name
                       and sys.all_indexes.index_name = sys.all_ind_columns.index_name
                       and sys.all_indexes.index_type = 'NORMAL'
                       and sys.all_indexes.status = 'VALID'
where lower(sys.all_tab_columns.table_name) like lower('AppDynamics_DefaultTable')
order by sys.all_tab_columns.column_id

drop table AppDynamics_DefaultTable;

select * from appdynamics_defaultTable;

create table ProxyAppEvents ( severity varchar2(20) NOT NULL, summary varchar2(120) NOT NULL, controller varchar2(50) NOT NULL,
application varchar2(50) NOT NULL, triggeredentityid number(22) NOT NULL, triggeredentitytype varchar2(50) NOT NULL,
eventtime number(22) NOT NULL, subtype varchar2(50) NOT NULL, id number(22) NOT NULL, triggeredentityname varchar2(50) NOT NULL,
type varchar2(50) NOT NULL, eventtimestamp date(7) NOT NULL)

drop table AppDynamics_Analytics_transactionDetails;


select * from AppDynamics_SchedulerControl;

select * from AppDynamics_Analytics_UniqueTransactionCount;

select * from AppDynamics_Analytics_transactionDetails;

select * from AppDynamics_Analytics_allTransactionDetails;

select * from ProxyAppEvents;

STATEMENT BATCH insert into AppD_An_CrossSellCreditoMobile (
    starttimestamp, endtimestamp, navegador, correoelectronico, numerotelefonico,
    fecha, usuario, completed, dispositivo, numerodecuenta,
    primeraoferta, segundaoferta, ofertaseleccionada, cuartaoferta, monto,
    tasadeinterïs, tipodeamortizaciïn, otp, fechadelprimerpago, motivodelcrïdito)
VALUES (
    TO_DATE('19700101','yyyymmdd') + (('1640107395113'/1000)/24/60/60),TO_DATE('19700101','yyyymmdd') + (('1640114595114'/1000)/24/60/60),'1640114083522','CROSSELL4','0',
    TO_DATE('19700101','yyyymmdd') + (('MB'/1000)/24/60/60),,,,
    ,,,,,
    ,,,,,)