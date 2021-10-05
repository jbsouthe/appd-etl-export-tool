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