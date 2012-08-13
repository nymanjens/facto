<?php

function backup_tables($tables) {
    $tables = is_array($tables) ? $tables : explode(',',$tables);
    $return = "";
    //cycle through
    foreach($tables as $table) {
        $result = query('SELECT * FROM '.$table);
        $num_fields = mysql_num_fields($result);
        
        $row2 = mysql_fetch_row(query('SHOW CREATE TABLE '.$table));
        $return.= "\n\n".$row2[1].";\n\n";
        
        for ($i = 0; $i < $num_fields; $i++)    {
            while($row = mysql_fetch_row($result)) {
                $return.= 'INSERT INTO '.$table.' VALUES(';
                for($j=0; $j<$num_fields; $j++)    {
                    $row[$j] = addslashes($row[$j]);
                    $row[$j] = ereg_replace("\n","\\n",$row[$j]);
                    if (isset($row[$j])) { $return.= '"'.$row[$j].'"' ; } else { $return.= '""'; }
                    if ($j<($num_fields-1)) { $return.= ','; }
                }
                $return.= ");\n";
            }
        }
        $return.="\n\n\n";
    }
    return $return;
}
